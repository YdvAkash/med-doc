package med.com.controllers;

import med.com.services.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String plan = request.get("plan");
            if (plan == null || (!plan.equals("BASIC") && !plan.equals("PRO"))) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid plan"));
            }
            
            String orderId = subscriptionService.createOrder(plan, authentication.getName());
            return ResponseEntity.ok(Map.of("success", true, "orderId", orderId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error creating order: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String orderId = request.get("razorpayOrderId");
            String paymentId = request.get("razorpayPaymentId");
            String signature = request.get("razorpaySignature");
            String plan = request.get("plan");

            if (subscriptionService.verifySignature(orderId, paymentId, signature)) {
                subscriptionService.upgradeUserTier(authentication.getName(), plan);
                return ResponseEntity.ok(Map.of("success", true, "message", "Payment verified and tier upgraded"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payment signature verification failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error verifying payment: " + e.getMessage()));
        }
    }
}
