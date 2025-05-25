package com.example.scanflow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RevisarFacturaFragment : Fragment() {

    private val contactos = mutableListOf<ContactoHolded>()
    private lateinit var cuentasFiltradas: List<CuentaContable>
    private var proveedorOriginalExtraido: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_revisar_factura, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            cargarContactosHolded()

            val archivo = requireArguments().getString("archivo") ?: return@launch
            val archivoJson = File(requireContext().filesDir, archivo)

            if (!archivoJson.exists()) {
                println("❌ Archivo no encontrado")
                return@launch
            }

            val contenido = archivoJson.readText()
            val jsonFactura = JSONObject(contenido)
            val proveedor = jsonFactura.getJSONObject("proveedor")
            val factura = jsonFactura.getJSONObject("factura")
            val proveedorExtraido = proveedor.optString("nombre", "")
            proveedorOriginalExtraido = proveedor.optString("nombre", "")
            val proveedorNormalizado = normalizarTexto(proveedorExtraido)

            var contactoDetectado: ContactoHolded? = null

            for (contacto in contactos) {
                val nombre = contacto.normalizado
                if (nombre.contains(proveedorNormalizado) || proveedorNormalizado.contains(nombre)) {
                    contactoDetectado = contacto
                    break
                } else {
                    val similitud = calcularSimilitud(nombre, proveedorNormalizado)
                    if (similitud > 0.7) {
                        contactoDetectado = contacto
                        break
                    }
                }
            }

            view.findViewById<EditText>(R.id.etNif).setText(proveedor.optString("nif"))
            view.findViewById<EditText>(R.id.etDireccion).setText(proveedor.optString("direccion"))
            view.findViewById<EditText>(R.id.etCP).setText(proveedor.optString("cp"))
            view.findViewById<EditText>(R.id.etPoblacion).setText(proveedor.optString("poblacion"))
            view.findViewById<EditText>(R.id.etTelefono).setText(proveedor.optString("telefono"))

            view.findViewById<EditText>(R.id.etNumero).setText(factura.optString("numero"))
            view.findViewById<EditText>(R.id.etFecha).setText(factura.optString("fecha_pedido"))

            val fechaPedidoStr = normalizarFecha(factura.optString("fecha_pedido"))
            val fechaVencimientoStr = normalizarFecha(factura.optString("fecha_vencimiento"))

            val fechaFinal = if (fechaVencimientoStr.isBlank()) {
                try {
                    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaPedido = formato.parse(fechaPedidoStr)
                    val calendar = java.util.Calendar.getInstance()
                    if (fechaPedido != null) {
                        calendar.time = fechaPedido
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                        formato.format(calendar.time)
                    } else ""
                } catch (e: Exception) {
                    ""
                }
            } else fechaVencimientoStr

            view.findViewById<EditText>(R.id.etVencimiento).setText(fechaFinal)

            view.findViewById<EditText>(R.id.etIVA).setText(factura.optString("iva"))
            view.findViewById<EditText>(R.id.etNeto).setText(factura.optString("importe_neto"))
            view.findViewById<EditText>(R.id.etTotal).setText(factura.optString("total_factura"))
            val spinner = view.findViewById<Spinner>(R.id.spinnerProveedor)

            /* lista de nombres para el Spinner */
            val nombresContactos = contactos.map { it.nombre }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                nombresContactos
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            val indexDetectado = contactoDetectado?.let { nombresContactos.indexOf(it.nombre) } ?: -1
            if (indexDetectado != -1) {
                spinner.setSelection(indexDetectado)
            } else {
                /* si no detecta ningún proveedor ponemos por defecto la opción de crear uno nuevo */
                val indexNuevo = nombresContactos.indexOf("➕ Crear nuevo contacto")
                if (indexNuevo != -1) {
                    spinner.setSelection(indexNuevo)
                }
            }

            val cuentas = obtenerCuentasContables()
            cuentasFiltradas = cuentas.filter {
                it.accountNum in listOf(
                    60000000,
                    62100000,
                    62300000,
                    62400000,
                    62700000,
                    62800000,
                    62900000
                )
            }

            val spinnerCuentas = view.findViewById<Spinner>(R.id.spinnerCuentaContable)
            val adapterCuentas = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cuentasFiltradas.map { "${it.accountNum} - ${it.name}" }
            )
            adapterCuentas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCuentas.adapter = adapterCuentas

            val indexSeleccionada = cuentasFiltradas.indexOfFirst { it.accountNum == 60000000 }
            if (indexSeleccionada != -1) {
                spinnerCuentas.setSelection(indexSeleccionada)
            }

            val spinnerTipo = view.findViewById<Spinner>(R.id.spinnerTipoFactura)
            val tipos =
                listOf("Factura normal" to "purchase", "Factura rectificativa" to "purchaserefund")
            val adapterTipo = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tipos.map { it.first })
            adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipo.adapter = adapterTipo

            view.findViewById<TextView>(R.id.avisoLineas).text =
                contactoDetectado?.nombre?.let { "✅ Proveedor detectado: $it" }
                    ?: "⚠️ No se detectó proveedor automáticamente: $proveedorExtraido"

            Log.d("prueba", "Proveedor detectado: $contactoDetectado")
        }

        view.findViewById<Button>(R.id.btnEnviar).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val btn = view.findViewById<Button>(R.id.btnEnviar)
                val progressBar = view.findViewById<ProgressBar>(R.id.progressBarEnvio)

                btn.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val contactoSeleccionado = contactos.find {
                    it.nombre == view.findViewById<Spinner>(R.id.spinnerProveedor).selectedItem.toString()
                }

                Log.d("prueba",contactoSeleccionado.toString())

                val nombreProveedor = if (contactoSeleccionado?.id == "nuevo") {
                    proveedorOriginalExtraido
                } else {
                    contactoSeleccionado?.nombre ?: "Proveedor desconocido"
                }

                Log.d("prueba",nombreProveedor)

                val proveedor = JSONObject().apply {
                    put("nombre", nombreProveedor)
                    put("nif", view.findViewById<EditText>(R.id.etNif).text.toString())
                    put("direccion", view.findViewById<EditText>(R.id.etDireccion).text.toString())
                    put("cp", view.findViewById<EditText>(R.id.etCP).text.toString())
                    put("poblacion", view.findViewById<EditText>(R.id.etPoblacion).text.toString())
                    put("telefono", view.findViewById<EditText>(R.id.etTelefono).text.toString())
                }

                val factura = JSONObject().apply {
                    put("numero", view.findViewById<EditText>(R.id.etNumero).text.toString())
                    put("fecha_pedido", view.findViewById<EditText>(R.id.etFecha).text.toString())
                    put("fecha_vencimiento", view.findViewById<EditText>(R.id.etVencimiento).text.toString())
                    put("iva", formatDecimal(view.findViewById<EditText>(R.id.etIVA).text.toString()))
                    put("importe_neto", formatDecimal(view.findViewById<EditText>(R.id.etNeto).text.toString()))
                    put("total_factura", formatDecimal(view.findViewById<EditText>(R.id.etTotal).text.toString()))
                }

                val tipoFactura =
                    view.findViewById<Spinner>(R.id.spinnerTipoFactura).selectedItemPosition.let {
                        if (it == 1) "purchaserefund" else "purchase"
                    }

                val contactoId = contactoSeleccionado?.id
                val cuentaContableId = obtenerCuentaSeleccionadaId(view)

                val jsonFinal = construirPayloadHolded( proveedor, factura, contactoId ?: "", cuentaContableId)

                Log.d("DEBUG_JSON_HOLDED", jsonFinal.toString(4))

                val success = enviarFacturaAHolded(jsonFinal, tipoFactura)
                var status = 0
                var id = ""

                success?.let {
                    status = it.optInt("status")
                    id = it.optString("id")
                }

                Toast.makeText(
                    requireContext(),
                    if (status == 1) "✅ Factura enviada correctamente" else "❌ Error al enviar la factura",
                    Toast.LENGTH_LONG
                ).show()

                if(status == 1){
                    val nombreJson = arguments?.getString("archivo") ?: "vacio"
                    val nombrePdf = nombreJson.replace(".json", ".pdf")
                    val pdfFile = File(requireContext().filesDir, nombrePdf)

                    if (pdfFile.exists()) {
                        subirPdfAHolded(id, pdfFile)
                    } else {
                        Log.e("PDF", "❌ No se encontró el archivo PDF: ${pdfFile.name}")
                    }

                    val fechaEnvio = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    val archivoJson = File(requireContext().filesDir, nombreJson)
                    val contenidoJson = JSONObject(archivoJson.readText())
                    contenidoJson.put("fecha_envio", fechaEnvio)
                    archivoJson.writeText(contenidoJson.toString(4))

                    val carpetaProcesadas = File(requireContext().filesDir, "procesadas")
                    if (!carpetaProcesadas.exists()) carpetaProcesadas.mkdirs()
                    val destino = File(carpetaProcesadas, archivoJson.name)

                    val movido = archivoJson.renameTo(destino)
                    Log.d("ARCHIVO", if (movido) "✅ JSON movido a carpeta procesadas" else "❌ No se pudo mover el JSON")

                    if (pdfFile.exists()) {
                        val borrado = pdfFile.delete()
                        Log.d("ARCHIVO", if (borrado) "✅ PDF borrado" else "❌ No se pudo borrar el PDF")
                    }

                    val fragment = PendientesFragment()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()


                }

                btn.isEnabled = true
                progressBar.visibility = View.GONE
            }
        }

    }

    private fun obtenerCuentaSeleccionadaId(view: View): String? {
        val spinner = view.findViewById<Spinner>(R.id.spinnerCuentaContable)
        val selectedText = spinner.selectedItem?.toString() ?: return null
        val cuentaNum = selectedText.split(" - ").firstOrNull()?.trim() ?: return null
        return cuentasFiltradas.find { it.accountNum.toString() == cuentaNum }?.id
    }

    private suspend fun cargarContactosHolded() = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.holded.com/api/invoicing/v1/contacts")
                .addHeader("accept", "application/json")
                .addHeader("key", "key")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val array = JSONArray(responseBody)

                contactos.clear()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val nombre = obj.optString("name")
                    if (id.isNotEmpty() && nombre.isNotEmpty()) {
                        contactos.add(
                            ContactoHolded(
                                id = id,
                                nombre = nombre,
                                normalizado = normalizarTexto(nombre)
                            )
                        )
                    }
                }
                contactos.add(
                    ContactoHolded(
                        id = "nuevo",
                        nombre = "➕ Crear nuevo contacto",
                        normalizado = "crear nuevo contacto"
                    )
                )
            } else {
                println("Error: ${response.code}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun obtenerCuentasContables(): List<CuentaContable> =
        withContext(Dispatchers.IO) {
            val cuentas = mutableListOf<CuentaContable>()
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.holded.com/api/invoicing/v1/expensesaccounts")
                    .addHeader("accept", "application/json")
                    .addHeader("key", "key")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val array = JSONArray(response.body?.string())
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        cuentas.add(
                            CuentaContable(
                                id = obj.getString("id"),
                                accountNum = obj.getInt("accountNum"),
                                name = obj.getString("name")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext cuentas
        }

    private fun normalizarTexto(texto: String): String {
        val sinTildes = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        return sinTildes.replace("[^a-z0-9 ]".toRegex(), "").trim()
    }

    fun normalizarFecha(fechaOriginal: String?): String {
        if (fechaOriginal.isNullOrBlank()) return ""

        val limpia = fechaOriginal.trim()
            .replace("-", "/")
            .replace(".", "/")
            .replace("\\s+".toRegex(), "")
            .replace(Regex("""^(\d{2}/\d{2}/)(\d{2})$""")) { match ->
                "${match.groupValues[1]}20${match.groupValues[2]}"
            }

        val formatosEntrada = listOf("dd/MM/yyyy", "yyyy/MM/dd")
        val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }

        for (formato in formatosEntrada) {
            try {
                val sdf = SimpleDateFormat(formato, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(limpia)
                if (date != null) return formatoSalida.format(date)
            } catch (_: Exception) {
            }
        }

        return ""
    }

    data class ContactoHolded(val id: String, val nombre: String, val normalizado: String)

    data class CuentaContable(val id: String, val accountNum: Int, val name: String)

    private fun calcularSimilitud(a: String, b: String): Double {
        val m = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in a.indices) m[i + 1][0] = i + 1
        for (j in b.indices) m[0][j + 1] = j + 1

        for (i in a.indices) {
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                m[i + 1][j + 1] = minOf(
                    m[i][j + 1] + 1,
                    m[i + 1][j] + 1,
                    m[i][j] + cost
                )
            }
        }

        val distancia = m[a.length][b.length]
        return 1.0 - distancia.toDouble() / maxOf(a.length, b.length)
    }

    private suspend fun construirPayloadHolded(
        proveedor: JSONObject,
        factura: JSONObject,
        contactoId: String?,
        cuentaContableId: String?,
    ): JSONObject {

        val client = OkHttpClient()
        val token = "key"

        var finalContactId = contactoId
        Log.d("Contacto ID", finalContactId.toString())

        if (finalContactId == "nuevo") {
            Log.d("Contacto ID", "nuevo")
            try {
                val nuevoContacto = JSONObject().apply {
                    put("name", proveedor.optString("nombre"))
                    put("code", proveedor.optString("nif"))
                    put("type", "supplier")
                    put("isperson", false)
                    put("phone", proveedor.optString("telefono"))
                    put("billAddress", JSONObject().apply {
                        put("address", proveedor.optString("direccion"))
                        put("city", proveedor.optString("poblacion"))
                        put("postalCode", proveedor.optString("cp"))
                    })
                }

                val request = Request.Builder()
                    .url("https://api.holded.com/api/invoicing/v1/contacts")
                    .addHeader("accept", "application/json")
                    .addHeader("key", token)
                    .addHeader("content-type", "application/json")
                    .post(
                        nuevoContacto.toString()
                            .toRequestBody("application/json".toMediaTypeOrNull())
                    )
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    val resBody = response.body?.string()
                    val jsonRes = JSONObject(resBody ?: "")
                    finalContactId = jsonRes.optString("id", null)
                    Log.d("Contacto ID", finalContactId.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fechas
        val fechaPedido = convertirFechaUnix(factura.optString("fecha_pedido"))
        var fechaVencimiento = convertirFechaUnix(factura.optString("fecha_vencimiento"))
        if (fechaVencimiento == null || fechaVencimiento == 0L) {
            fechaVencimiento = fechaPedido?.plus(86400)
        }

        val neto = limpiarNumeroEuropeo(factura.optString("importe_neto"))
        val iva = limpiarNumeroEuropeo(factura.optString("iva"))

        val items = JSONArray().apply {
            put(JSONObject().apply {
                put("name", proveedor.optString("nombre"))
                put("quantity", 1)
                put("subtotal", "%.2f".format(neto))
                put("tax", "%.2f".format(iva))
            })
        }

        return JSONObject().apply {
            put("desc", proveedor.optString("nombre"))
            put("date", fechaPedido ?: 0)
            put("dueDate", fechaVencimiento ?: 0)
            put("notes", "Factura generada automáticamente")
            put("items", items)
            put("approveDoc", true)
            put("invoiceNum", factura.optString("numero"))
            put("contactId", finalContactId)
            cuentaContableId?.let { put("salesChannelId", it) }
        }
    }

    private suspend fun enviarFacturaAHolded(
        payload: JSONObject,
        tipoFactura: String /* "purchase" o "purchaserefund" */
    ): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val token = "key"

                val request = Request.Builder()
                    .url("https://api.holded.com/api/invoicing/v1/documents/$tipoFactura")
                    .addHeader("accept", "application/json")
                    .addHeader("key", token)
                    .addHeader("content-type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    Log.d("FACTURA", "✅ Factura enviada: $json")
                    return@withContext json
                } else {
                    Log.e("FACTURA", "❌ Error ${response.code}: ${response.message}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("FACTURA", "❌ Excepción al enviar factura", e)
                return@withContext null
            }
        }
    }

    private fun limpiarNumeroEuropeo(numero: String): Float {
        var limpio = numero.trim()
        if (limpio.contains(",") && limpio.contains(".")) {
            limpio = if (limpio.indexOf(',') > limpio.indexOf('.')) {
                limpio.replace(".", "").replace(",", ".")
            } else {
                limpio.replace(",", "")
            }
        } else {
            limpio = limpio.replace(",", ".")
        }
        return limpio.toFloatOrNull() ?: 0f
    }

    private fun convertirFechaUnix(fecha: String?): Long? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(fecha ?: return null)
            date?.time?.div(1000)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun subirPdfAHolded(documentId: String, pdfFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = "key"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    pdfFile.name,
                    pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                )
                .addFormDataPart("setMain", "true")
                .build()

            val request = Request.Builder()
                .url("https://api.holded.com/api/invoicing/v1/documents/purchase/$documentId/attach")
                .addHeader("key", token)
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            val responseBody = response.body?.string()
            if (response.isSuccessful) {
                Log.d("PDF", "✅ PDF subido correctamente")
                Log.d("PDF", "Respuesta: $responseBody")
                return@withContext true
            } else {
                Log.e("PDF", "❌ Error al subir PDF: ${response.code} - ${response.message}")
                Log.e("PDF", "Cuerpo respuesta: $responseBody")
            }

        } catch (e: Exception) {
            Log.e("PDF", "❌ Excepción al subir PDF", e)
        }

        return@withContext false
    }

    fun formatDecimal(valor: String): String {
        return valor
            .replace(",", ".")
            .toDoubleOrNull()
            ?.let { String.format(Locale.US, "%.2f", it) }
            ?: "0.00"
    }

}
