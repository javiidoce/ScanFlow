package com.example.scanflow

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File

class PendientesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnEliminar: Button
    private lateinit var adapter: FacturaPendienteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_pendientes, container, false)
        recyclerView = view.findViewById(R.id.recyclerPendientes)
        btnEliminar = view.findViewById(R.id.btnEliminar)

        recyclerView.layoutManager = LinearLayoutManager(context)

        btnEliminar.setOnClickListener {
            val seleccionadas = adapter.obtenerSeleccionadas()
            if (seleccionadas.isEmpty()) {
                Toast.makeText(context, "No hay archivos seleccionados", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            seleccionadas.forEach { /* borramos el pdf y el json del archivo seleccionado */
                it.file.delete()
                val pdf = File(it.file.parent, it.file.nameWithoutExtension + ".pdf")
                if (pdf.exists()) pdf.delete()
            }

            Toast.makeText(context, "Facturas eliminadas", Toast.LENGTH_SHORT).show()
            mostrarFacturasPendientes() /* actualizamos la lista */
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        mostrarFacturasPendientes()
    }

    private fun mostrarFacturasPendientes() {
        val files = context?.filesDir?.listFiles()?.filter {
            it.extension == "json"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        val facturas = files.map {
            try {
                val json = JSONObject(it.readText())
                val proveedor = json.optJSONObject("proveedor")?.optString("nombre") ?: "Desconocido"
                val fecha = json.optJSONObject("factura")?.optString("fecha_pedido") ?: "¿?"
                FacturaPendiente(it.name, proveedor, fecha, it)
            } catch (e: Exception) {
                Log.e("FACTURA_JSON_ERROR", "Error leyendo ${it.name}: ${e.message}")
                FacturaPendiente(it.name, "Error", "Error", it)
            }
        }

        adapter = FacturaPendienteAdapter(facturas) { factura ->
            val fragment = RevisarFacturaFragment().apply {
                arguments = Bundle().apply {
                    putString("archivo", factura.nombreArchivo)
                }
            }
            (activity as MainActivity).loadFragment(fragment)
        }


        recyclerView.adapter = adapter
    }

}
