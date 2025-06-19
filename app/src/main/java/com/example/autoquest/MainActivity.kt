package com.example.autoquest

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.autoquest.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationBarView

class MainActivity() : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())


        // переключение фрагментов
        binding!!.bottomBar.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.search) {
                replaceFragment(SearchFragment())
            } else if (item.itemId == R.id.favorites) {
                replaceFragment(FavoritesFragment())
            } else if (item.itemId == R.id.home) {
                replaceFragment(HomeFragment())
            } else if (item.itemId == R.id.info) {
                replaceFragment(InformationFragment())
            } else if (item.itemId == R.id.profile) {
                replaceFragment(ProfileFragment())
            }
            true
        }
        // уставнока кнопки home по умолчанию при открытии приложения
        binding!!.bottomBar.setSelectedItemId(R.id.home)
        replaceFragment(HomeFragment())
    }

    override fun onPause() {
        super.onPause()
    }

    // функция переключения фрагмента
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }

    companion object {
        const val LOG: String = "MainActivity"
    }
}