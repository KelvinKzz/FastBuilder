package com.gaoding.fastbuilder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.gaoding.fastbuilder.library.TestKt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.e("CWQ", "MainActivity" + OTest.test())
        findViewById<TextView>(R.id.tv2).text = TestKt.test()
        findViewById<TextView>(R.id.tv).text = getString(R.string.mark_picture_property_replace)
    }

    override fun onResume() {
        super.onResume()
        Log.e("CWQ", "MainActivity onResume" + getString(R.string.test_string))
    }
}