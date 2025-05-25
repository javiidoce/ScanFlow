package com.example.scanflow

import java.io.File

data class FacturaProcesada(
    val nombreArchivo: String,
    val proveedor: String,
    val fecha: String,
    val file: File
)
