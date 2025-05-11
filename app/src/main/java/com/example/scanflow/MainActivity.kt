package com.example.scanflow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        loadFragment(InicioFragment()) /* cargamos el fragmento de inicio por defecto */

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_pendientes -> loadFragment(PendientesFragment())
                R.id.nav_inicio -> loadFragment(InicioFragment())
                R.id.nav_procesadas -> loadFragment(ProcesadasFragment())
            }
            true
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}