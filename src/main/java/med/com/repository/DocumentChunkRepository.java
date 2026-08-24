package med.com.repository;

import med.com.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, Long> {

    @Modifying
    @Query(value = "INSERT INTO document_chunks (document_id, chunk_text, embedding, created_at) " +
                   "VALUES (:documentId, :chunkText, CAST(:embedding AS vector), NOW())", nativeQuery = true)
    void insertChunkWithVector(@Param("documentId") Long documentId, 
                               @Param("chunkText") String chunkText, 
                               @Param("embedding") String embeddingVectorString);

    // Find the closest chunks by Cosine Distance using pgvector <=> operator
    // NeonDB pgvector supports <=> (cosine), <-> (L2), <#> (inner product)
    @Query(value = "SELECT c.* FROM document_chunks c " +
                   "JOIN documents d ON c.document_id = d.id " +
                   "WHERE d.user_id = :userId " +
                   "ORDER BY c.embedding <=> CAST(:embedding AS vector) ASC " +
                   "LIMIT :limit", nativeQuery = true)
    List<DocumentChunkEntity> findSimilarChunks(@Param("userId") Long userId, 
                                                @Param("embedding") String embeddingVectorString, 
                                                @Param("limit") int limit);
}
