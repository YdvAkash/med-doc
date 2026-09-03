package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.entity.BannerEntity;
import med.com.repository.BannerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BannerController {

    private final BannerRepository bannerRepository;

    // Public endpoint for mobile app
    @GetMapping("/banners/active")
    public ResponseEntity<List<BannerEntity>> getActiveBanners() {
        return ResponseEntity.ok(bannerRepository.findByIsActiveTrueOrderByCreatedAtDesc());
    }

    // Admin endpoints
    @GetMapping("/admin/banners")
    public ResponseEntity<List<BannerEntity>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAll());
    }

    @PostMapping("/admin/banners")
    public ResponseEntity<BannerEntity> createBanner(@RequestBody BannerEntity banner) {
        if (banner.getIsActive() == null) {
            banner.setIsActive(true);
        }
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    @PutMapping("/admin/banners/{id}")
    public ResponseEntity<BannerEntity> updateBanner(@PathVariable Long id, @RequestBody BannerEntity bannerDetails) {
        Optional<BannerEntity> optionalBanner = bannerRepository.findById(id);
        if (optionalBanner.isPresent()) {
            BannerEntity banner = optionalBanner.get();
            if (bannerDetails.getTitle() != null) banner.setTitle(bannerDetails.getTitle());
            if (bannerDetails.getImageUrl() != null) banner.setImageUrl(bannerDetails.getImageUrl());
            if (bannerDetails.getHtmlContent() != null) banner.setHtmlContent(bannerDetails.getHtmlContent());
            if (bannerDetails.getActionUrl() != null) banner.setActionUrl(bannerDetails.getActionUrl());
            if (bannerDetails.getIsActive() != null) banner.setIsActive(bannerDetails.getIsActive());
            return ResponseEntity.ok(bannerRepository.save(banner));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/admin/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
