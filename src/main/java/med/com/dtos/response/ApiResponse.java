package med.com.dtos.response;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private int status;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data, String error, int status) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.status = status;
    }

    // Helper method for successful responses
    public static <T> ApiResponse<T> success(T data, int status) {
        return new ApiResponse<>(true, data, null, status);
    }

    // Helper method for successful responses (default 200 OK)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, 200);
    }

    // Helper method for error responses
    public static <T> ApiResponse<T> error(String error, int status) {
        return new ApiResponse<>(false, null, error, status);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
