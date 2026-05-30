import http from "./http";

export function uploadImageApi(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/upload/image", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
}

export function uploadMediaApi(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/upload/media", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
}
