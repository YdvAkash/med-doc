package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.dtos.request.CreateFolderRequest;
import med.com.dtos.response.ApiResponse;
import med.com.dtos.response.FolderResponse;
import med.com.services.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getFolders(Principal principal) {
        List<FolderResponse> folders = folderService.getUserFolders(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(folders));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @RequestBody CreateFolderRequest request,
            Principal principal
    ) {
        FolderResponse response = folderService.createFolder(request.getName(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{folderId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> addDocumentToFolder(
            @PathVariable Long folderId,
            @PathVariable Long documentId,
            Principal principal
    ) {
        folderService.addDocumentToFolder(folderId, documentId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
