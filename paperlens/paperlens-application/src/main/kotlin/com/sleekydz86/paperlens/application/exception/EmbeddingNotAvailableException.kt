package com.sleekydz86.paperlens.application.exception

class EmbeddingNotAvailableException(
    message: String = "임베딩이 비활성화되어 있습니다. 프로필 'embedding'으로 기동하고 유효한 ONNX 모델 URI를 설정하세요.",
) : RuntimeException(message)
