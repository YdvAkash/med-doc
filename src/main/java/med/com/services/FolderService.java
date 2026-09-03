package med.com.services;

import lombok.RequiredArgsConstructor;
import med.com.dtos.response.FolderResponse;
import med.com.entity.DocumentEntity;
import med.com.entity.FolderEntity;
import med.com.entity.UserEntity;
import med.com.exceptions.ResourceNotFoundException;
import med.com.repository.DocumentRepository;
import med.com.repository.FolderRepository;
import med.com.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public List<FolderResponse> getUserFolders(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        return folderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FolderResponse createFolder(String name, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        FolderEntity folder = FolderEntity.builder()
                .name(name)
                .user(user)
                .build();

        FolderEntity saved = folderRepository.save(folder);
        return toResponse(saved);
    }

    @Transactional
    public void addDocumentToFolder(Long folderId, Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        FolderEntity folder = folderRepository.findByIdAndUserId(folderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FOLDER_NOT_FOUND", "Folder not found."));

        DocumentEntity document = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "Document not found."));

        // ManyToMany logic
        if (!folder.getDocuments().contains(document)) {
            folder.getDocuments().add(document);
            folderRepository.save(folder);
        }
    }

    private FolderResponse toResponse(FolderEntity folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .createdAt(folder.getCreatedAt())
                .documentCount(folder.getDocuments() != null ? folder.getDocuments().size() : 0)
                .build();
    }
}
