package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.annotation.BindView
import com.example.binder.Binding

class MainActivity : AppCompatActivity() {

    @BindView(R.id.tv)
    lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Binding.bind(this)
    }
}