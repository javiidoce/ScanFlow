package com.example.scanflow

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.util.*

class ProcesadasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FacturaProcesadaAdapter
    private var facturasTotales: List<FacturaProcesada> = emptyList()

    private var fechaInicio: String? = null
    private var fechaFin: String? = null

    private lateinit var btnFechaInicio: Button
    private lateinit var btnFechaFin: Button
    private lateinit var btnLimpiarFiltro: Button
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_procesadas, container, false)
        recyclerView = view.findViewById(R.id.recyclerProcesadas)
        recyclerView.layoutManager = LinearLayoutManager(context)

        searchView = view.findViewById(R.id.searchProveedor)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarPorProveedor(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarPorProveedor(newText)
                return true
            }
        })

        btnFechaInicio = view.findViewById(R.id.btnFechaInicio)
        btnFechaFin = view.findViewById(R.id.btnFechaFin)
        btnLimpiarFiltro = view.findViewById(R.id.btnLimpiarFiltro)

        btnFechaInicio.setOnClickListener {
            mostrarSelectorFecha(fechaInicio) { fecha ->
                /* si fecha fin existe y la nueva inicio es mayor que fin, error */
                if (fechaFin != null) {
                    val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val nuevaInicio = formatoFecha.parse(fecha)
                    val fin = formatoFecha.parse(fechaFin)
                    if (nuevaInicio.after(fin)) {
                        Toast.makeText(context, "La fecha inicio no puede ser después de la fecha fin", Toast.LENGTH_SHORT).show()
                        return@mostrarSelectorFecha
                    }
                }
                fechaInicio = fecha
                btnFechaInicio.text = "Inicio: $fecha"
                filtrarFacturas()
            }
        }

        btnFechaFin.setOnClickListener {
            mostrarSelectorFecha(fechaFin) { fecha ->
                /* lo mismo que antes */
                if (fechaInicio != null) {
                    val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val inicio = formatoFecha.parse(fechaInicio)
                    val nuevaFin = formatoFecha.parse(fecha)
                    if (nuevaFin.before(inicio)) {
                        Toast.makeText(context, "La fecha fin no puede ser antes de la fecha inicio", Toast.LENGTH_SHORT).show()
                        return@mostrarSelectorFecha
                    }
                }
                fechaFin = fecha
                btnFechaFin.text = "Fin: $fecha"
                filtrarFacturas()
            }
        }

        btnLimpiarFiltro.setOnClickListener {
            fechaInicio = null
            fechaFin = null
            btnFechaInicio.text = "Fecha inicio"
            btnFechaFin.text = "Fecha fin"
            mostrarListaFiltrada(facturasTotales)
            Toast.makeText(context, "Filtros limpiados", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        mostrarFacturasProcesadas()
    }

    private fun mostrarFacturasProcesadas() {
        val dir = File(requireContext().filesDir, "procesadas")
        if (!dir.exists()) {
            Toast.makeText(context, "No hay facturas procesadas", Toast.LENGTH_SHORT).show()
            return
        }

        val archivos = dir.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

        facturasTotales = archivos.mapNotNull { file ->
            try {
                val json = JSONObject(file.readText())
                val proveedor = json.optJSONObject("proveedor")?.optString("nombre") ?: "Desconocido"
                val fecha = json.optJSONObject("factura")?.optString("fecha_pedido") ?: "¿?"
                FacturaProcesada(file.nameWithoutExtension, proveedor, fecha, file)
            } catch (e: Exception) {
                Log.e("JSON_ERROR", "Error leyendo ${file.name}: ${e.message}")
                null
            }
        }

        filtrarFacturas()
    }

    private fun mostrarSelectorFecha(fechaActual: String?, onFechaSeleccionada: (String) -> Unit) {
        val calendario = Calendar.getInstance()

        /* si hay fecha actual, la ponemos en el calendario para que el datepicker arranque desde ahí */
        fechaActual?.let {
            try {
                val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = formatoFecha.parse(it)
                if (date != null) {
                    calendario.time = date
                }
            } catch (e: Exception) {
            }
        }

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                onFechaSeleccionada(fechaSeleccionada)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun filtrarFacturas() {
        val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val filtradas = facturasTotales.filter { factura ->
            try {
                val fechaFactura = formatoFecha.parse(factura.fecha) ?: return@filter false
                val inicio = fechaInicio?.let { formatoFecha.parse(it) }
                val fin = fechaFin?.let { formatoFecha.parse(it) }

                val despuesDeInicio = inicio?.let { !fechaFactura.before(it) } ?: true
                val antesDeFin = fin?.let { !fechaFactura.after(it) } ?: true

                despuesDeInicio && antesDeFin
            } catch (e: Exception) {
                false
            }
        }

        if (filtradas.isEmpty()) { /* no aparecen??*/ /* (revisar) */
            Toast.makeText(context, "No hay facturas para el rango seleccionado", Toast.LENGTH_SHORT).show()
        }

        mostrarListaFiltrada(filtradas)
    }

    private fun mostrarListaFiltrada(lista: List<FacturaProcesada>) {
        adapter = FacturaProcesadaAdapter(lista) { factura ->
            val fragment = DetalleFacturaProcesadaFragment().apply {
                arguments = Bundle().apply {
                    putString("archivo", factura.nombreArchivo)
                }
            }
            (activity as MainActivity).loadFragment(fragment)
        }
        recyclerView.adapter = adapter
    }

    private fun filtrarPorProveedor(texto: String?) {
        val textoMinus = texto?.lowercase(Locale.getDefault()) ?: ""

        /* filtramos por proveedor y rango de fechas */ /* revisar funcionamiento del rango */
        val filtradas = facturasTotales.filter { factura ->
            val proveedorMatch = factura.proveedor.lowercase(Locale.getDefault()).contains(textoMinus)

            val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaFactura = try {
                formatoFecha.parse(factura.fecha)
            } catch (e: Exception) {
                null
            }

            val fechaInicioDate = fechaInicio?.let {
                formatoFecha.parse(it)
            }
            val fechaFinDate = fechaFin?.let {
                formatoFecha.parse(it)
            }

            val dentroRango = when {
                fechaInicioDate != null && fechaFinDate != null -> {
                    fechaFactura != null && !fechaFactura.before(fechaInicioDate) && !fechaFactura.after(fechaFinDate)
                }
                fechaInicioDate != null -> {
                    fechaFactura != null && !fechaFactura.before(fechaInicioDate)
                }
                fechaFinDate != null -> {
                    fechaFactura != null && !fechaFactura.after(fechaFinDate)
                }
                else -> true
            }

            proveedorMatch && dentroRango
        }

        if (filtradas.isEmpty()) {
            Toast.makeText(context, "No hay facturas que coincidan", Toast.LENGTH_SHORT).show()
        }

        mostrarListaFiltrada(filtradas)
    }

}

