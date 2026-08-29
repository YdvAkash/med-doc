package med.com.services;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import med.com.entity.UserEntity;
import med.com.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SubscriptionService {

    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;
    private final String razorpaySecret;

    public SubscriptionService(
            UserRepository userRepository,
            @Value("${razorpay.key.id}") String razorpayKeyId,
            @Value("${razorpay.key.secret}") String razorpaySecret
    ) throws Exception {
        this.userRepository = userRepository;
        this.razorpaySecret = razorpaySecret;
        // Handle placeholder values for development
        if ("YOUR_RAZORPAY_KEY_ID".equals(razorpayKeyId) || "YOUR_RAZORPAY_KEY_SECRET".equals(razorpaySecret)) {
             this.razorpayClient = null; // Mock mode
        } else {
             this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpaySecret);
        }
    }

    public String createOrder(String plan, String userEmail) throws Exception {
        if (this.razorpayClient == null) {
            // Mock order ID for testing if keys are not provided
            return "order_mock_" + System.currentTimeMillis();
        }

        int amount = plan.equals("PRO") ? 39900 : 19900; // in paise (₹399 or ₹199)
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());
        Order order = razorpayClient.orders.create(orderRequest);
        return order.get("id");
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (this.razorpayClient == null) return true; // Accept anything in mock mode
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(options, razorpaySecret);
        } catch (Exception e) {
            return false;
        }
    }

    public void upgradeUserTier(String email, String tier) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        user.setSubscriptionTier(tier);
        user.setReportsUploadedThisWeek(0);
        user.setChatsThisWeek(0);
        user.setLimitResetDate(LocalDate.now());
        user.setChatLimitResetDate(LocalDate.now());
        userRepository.save(user);
    }

    private void resetDocumentLimitsIfNeeded(UserEntity user) {
        if (user.getLimitResetDate() == null || user.getLimitResetDate().plusMonths(1).isBefore(LocalDate.now())) {
            user.setReportsUploadedThisWeek(0); // This is now used as reports uploaded this month
            user.setLimitResetDate(LocalDate.now());
            userRepository.save(user);
        }
    }

    private void resetChatLimitsIfNeeded(UserEntity user) {
        if (user.getChatLimitResetDate() == null || user.getChatLimitResetDate().plusMonths(1).isBefore(LocalDate.now())) {
            user.setChatsThisWeek(0); // This is now used as chats this month
            user.setChatLimitResetDate(LocalDate.now());
            userRepository.save(user);
        }
    }

    public boolean canUploadDocument(UserEntity user) {
        resetDocumentLimitsIfNeeded(user);
        if ("PRO".equals(user.getSubscriptionTier())) return true;
        if ("BASIC".equals(user.getSubscriptionTier())) return user.getReportsUploadedThisWeek() < 7;
        return user.getReportsUploadedThisWeek() < 3; // FREE
    }

    public void incrementUploadCount(UserEntity user) {
        user.setReportsUploadedThisWeek(user.getReportsUploadedThisWeek() + 1);
        userRepository.save(user);
    }

    public boolean canChat(UserEntity user) {
        resetChatLimitsIfNeeded(user);
        if ("PRO".equals(user.getSubscriptionTier())) return true;
        if ("BASIC".equals(user.getSubscriptionTier())) return user.getChatsThisWeek() < 20;
        return user.getChatsThisWeek() < 2; // FREE
    }

    public void incrementChatCount(UserEntity user) {
        user.setChatsThisWeek(user.getChatsThisWeek() + 1);
        userRepository.save(user);
    }
}
