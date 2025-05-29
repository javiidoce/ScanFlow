package com.example.scanflow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.auth.oauth2.ServiceAccountCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale

class InicioFragment : Fragment() {

    private lateinit var btnSeleccionar: Button
    private lateinit var btnProcesar: Button
    private lateinit var archivoSeleccionado: TextView
    private lateinit var progreso: ProgressBar
    private lateinit var listaFacturas: RecyclerView
    private lateinit var btnFoto: Button
    private val REQUEST_IMAGE_CAPTURE = 1002
    private lateinit var currentPhotoPath: String

    private var archivoUri: Uri? = null

    private var prompt = "Quiero que extraigas los siguientes datos de esta factura.\n" +
            "\n" +
            "IMPORTANTE:\n" +
            "- \"Gama Hostelería\" o \"GAMA 1988 SRL\" **NO** es el proveedor, es el cliente.\n" +
            "- El proveedor de esta factura **probablemente** sea uno de los siguientes:\n" +
            "\n" +
            "ABC, ADLER, ARILEX, Ascaso, BARTSCHER, Blendtec, Braher, Campeona, CASFRI, Casselin, Clima Hosteleria, Cofrise steel, Colged, Coreco, COREQUIP, DIFRIHO, DISTFORM, Docriluc, Dynamic, Easyline, EDENOX, ELECTROLUX, Elframo, ERATOS, Eunasa, EUROCORT, EUROFRED, Eutron, FECA, FEYMA, FIAMMA, DueEffe A, DueEffe BC, DueEffe D, DueEffe E, FM INDUSTRIAL, FRED, FRICOSMOS, Frucosol, Gasfrit, GRAM, Hamiltonbeach, HENDI, HR FAINCA, IDEACER, INFRICO, INOXFACTORY, IRIMAR, ITV, Jemi, JOSPER, Lacor, LOMI, MAINCA, MAINHO, MAISER, Maquinox, MEDOC, MERAL, MIZUMO, Movilfrit, MUEBLES ROMERO, Mundigas, Mychef, NIKROM, Ntgas, Orved, PUJADAS, Qualityfry, RATIONAL, REPAGAS, ROBOT COUPE, ROMAGSA, ROMUX, SAMMIC, SAVEMAH, SAYL, Sudimp, Total Grill, UNOX, VARIOCOOKING, VITRINAS GOMEZ, VITRINAS GOMEZ N, ZOWN, ZUMEX, ZUMMO, Tefcold, PEB Machinery, Stalgast.\n" +
            "\n" +
            "\uD83D\uDD0E Extrae los datos en formato JSON con la siguiente estructura exacta:\n" +
            "\n" +
            "{\n" +
            "  \"proveedor\": {\n" +
            "    \"nombre\": \"\",\n" +
            "    \"nif\": \"\",\n" +
            "    \"direccion\": \"\",\n" +
            "    \"cp\": \"\",\n" +
            "    \"poblacion\": \"\",\n" +
            "    \"telefono\": \"\"\n" +
            "  },\n" +
            "  \"lineas\": [\n" +
            "    {\n" +
            "      \"descripcion\": \"\",\n" +
            "      \"cantidad\": \"\",\n" +
            "      \"precio_unitario\": \"\",\n" +
            "      \"descuento_porcentaje\": \"\",\n" +
            "      \"subtotal_linea\": \"\",\n" +
            "      \"total_linea\": \"\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"factura\": {\n" +
            "    \"numero\": \"\",\n" +
            "    \"fecha_pedido\": \"\",\n" +
            "    \"fecha_vencimiento\": \"\",\n" +
            "    \"iva\": \"\",\n" +
            "    \"importe_neto\": \"\",\n" +
            "    \"total_factura\": \"\"\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "\uD83D\uDCDD Instrucciones adicionales:\n" +
            "- Si un dato no aparece claramente, usa null.\n" +
            "- No inventes información.\n" +
            "- El campo total_factura debe coincidir con la suma de todas las líneas.\n" +
            "- Devuelve **solo** un JSON válido. Nada más.\n" +
            "- El dato \"iva\" de factura tiene que ser el porcentaje aplicado, no la cantidad.\n" +
            "- El dato fecha pedido puede ser solo fecha, pero debe ser la fecha de la factura, no la de vencimiento."

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        btnSeleccionar = view.findViewById(R.id.btnSeleccionar)
        btnProcesar = view.findViewById(R.id.btnProcesar)
        archivoSeleccionado = view.findViewById(R.id.archivoSeleccionado)
        progreso = view.findViewById(R.id.progreso)
        listaFacturas = view.findViewById(R.id.listaFacturas)
        btnFoto = view.findViewById(R.id.btnFoto)
        btnFoto.setOnClickListener {
            dispatchTakePictureIntent()
        }

        listaFacturas.layoutManager = LinearLayoutManager(context)

        mostrarFacturasPendientes()

        btnSeleccionar.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Seleccionar factura"), 1001)
        }

        btnProcesar.setOnClickListener {
            if (archivoUri == null) {
                Toast.makeText(context, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progreso.visibility = View.VISIBLE

            lifecycleScope.launch {
                val base64 = uriToBase64(requireContext(), archivoUri!!)
                if (base64 == null) {
                    Toast.makeText(context, "Error al leer el archivo", Toast.LENGTH_LONG).show()
                    progreso.visibility = View.GONE
                    return@launch
                }

                val resultado = procesarFactura(requireContext(), base64, prompt)
                val nombreBase = "factura_${System.currentTimeMillis()}"
                if (resultado != null) {
                    guardarFacturaPendiente(requireContext(), resultado, nombreBase)
                    guardarPdfLocal(requireContext(), archivoUri!!, nombreBase)
                    Toast.makeText(context, "Factura procesada y guardada", Toast.LENGTH_LONG).show()
                    mostrarFacturasPendientes()
                } else {
                    Toast.makeText(context, "Error al procesar con Vertex AI", Toast.LENGTH_LONG).show()
                }

                progreso.visibility = View.GONE
            }
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) { /* según subamos un archivo o una foto hacemos una acción u otra */
            archivoUri = data?.data
            archivoSeleccionado.text = archivoUri?.lastPathSegment ?: "Archivo seleccionado"
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) { /*la única diferencia es que si subimos una imagen la convertimos*/
            val fotoFile = File(currentPhotoPath)                                               /* a pdf */
            val pdfTemp = convertirImagenAPdf(requireContext(), fotoFile, "temporal")

            if (pdfTemp != null) {
                archivoUri = Uri.fromFile(pdfTemp)
                archivoSeleccionado.text = pdfTemp.name
            } else {
                Toast.makeText(context, "Error al convertir imagen a PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun uriToBase64(context: Context, uri: Uri): String? { /* convierte el archivo a base64 (en principio necesario para pasarlo a la api de holded?) */
        return try {                                        /* (revisar) */
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return null
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    suspend fun obtenerAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try { /* pilla las credenciales y las manda a la api de google para que nos den el pase para utilizarlas */
            val assetManager = context.assets
            val inputStream = assetManager.open("cre.json")
            val credentials = ServiceAccountCredentials.fromStream(inputStream)
            val scoped = credentials.createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            scoped.refreshIfExpired()
            scoped.accessToken.tokenValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun procesarFactura(context: Context, base64Data: String, prompt: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val token = obtenerAccessToken(context) ?: return@withContext null

            val body = mapOf( /* preparamos el contenido que enviamos a la api de google para leer la factura */
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf("text" to prompt), /* le pasamos el prompt (texto de lo que tiene que hacer) */
                            mapOf( /* le pasamos el pdf */
                                "inlineData" to mapOf(
                                    "mimeType" to "application/pdf",
                                    "data" to base64Data
                                )
                            )
                        )
                    )
                )
            )

            val client = OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val jsonString = JSONObject(body).toString()
            val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://us-central1-aiplatform.googleapis.com/v1/projects/api-facturas-452709/locations/us-central1/publishers/google/models/gemini-1.5-pro-002:generateContent")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respuestaTexto = response.body?.string()
                Log.d("Respuesta API", respuestaTexto.toString())
                val candidates = JSONObject(respuestaTexto).getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        val textPart = parts.getJSONObject(0).getString("text")
                        Log.d("Texto Extraído Crudo", textPart)

                        /* eliminar las etiquetas ```json, ``` y los huecos que haya en la respuesta */
                        val cleanedText = textPart
                            .replace("```json", "", ignoreCase = true)
                            .replace("```", "", ignoreCase = true)
                            .trim()

                        Log.d("Texto Extraído Limpio", cleanedText)

                        return@withContext try {
                            JSONObject(cleanedText)
                        } catch (e: Exception) {
                            Log.e("Error Parseo JSON", "No se pudo parsear: $cleanedText", e)
                            null
                        }
                    } else {
                        Log.w("Advertencia", "La lista de 'parts' está vacía.")
                    }
                } else {
                    Log.w("Advertencia", "La lista de 'candidates' está vacía.")
                }
            } else {
                Log.e("Error API", "La llamada a la API no fue exitosa: ${response.code}")
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun guardarFacturaPendiente(context: Context, json: JSONObject, nombre: String) { /* guardar el json limpio de la factura */
        val file = File(context.filesDir, "$nombre.json")
        file.writeText(json.toString(2))
        Log.d("Guardado", "Archivo $nombre.json guardado correctamente en ${file.absolutePath}")
    }

    fun guardarPdfLocal(context: Context, uri: Uri, nombreBase: String): Boolean { /* guardar el pdf que hemos subido */
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val pdfFile = File(context.filesDir, "$nombreBase.pdf")
            val outputStream = pdfFile.outputStream()
            inputStream?.copyTo(outputStream)
            outputStream.close()
            inputStream?.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun mostrarFacturasPendientes() { /* cargamos todos los json que estan en espera de ser subidos y los cargamos por pantalla */
        val files = context?.filesDir?.listFiles()?.filter {
            it.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        listaFacturas.adapter = FacturaAdapter(files)
    }

    fun convertirImagenAPdf(context: Context, imagenFile: File, nombreBase: String): File? {
        return try { /* paso la imagen a pdf para poder subirlo a holded */
            val originalBitmap = BitmapFactory.decodeFile(imagenFile.absolutePath)
            val bitmap = originalBitmap.scale( /* bajo la resolución de la imagen (sino puede llegar a pesar 40 mb, revisar esto) */
                1000,
                (originalBitmap.height * (1000f / originalBitmap.width)).toInt()
            )

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)

            val pdfFile = File(context.filesDir, "$nombreBase.pdf")
            pdfFile.outputStream().use { outputStream ->
                document.writeTo(outputStream)
            }

            document.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}