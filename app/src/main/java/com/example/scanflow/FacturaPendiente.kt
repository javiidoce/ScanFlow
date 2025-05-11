package com.example.scanflow

import java.io.File

data class FacturaPendiente(
    val nombreArchivo: String,
    val proveedor: String,
    val fecha: String,
    val file: File,
    var seleccionado: Boolean = false
)
