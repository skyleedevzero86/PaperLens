package com.sleekydz86.paperlens.application.exception

class EmbeddingNotAvailableException(
    message: String = "Embedding model is unavailable. Start with embedding enabled and configure a valid ONNX model URI.",
) : RuntimeException(message)
