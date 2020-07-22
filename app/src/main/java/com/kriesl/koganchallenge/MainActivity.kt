package com.kriesl.koganchallenge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var filteredProducts = JSONArray()
    private val convertFactor = 250

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getJSON("/api/products/1")
    }

    private fun getJSON(url: String) {
        val queue = Volley.newRequestQueue(this)
        val hostURL = "http://wp8m3he1wt.s3-website-ap-southeast-2.amazonaws.com"
        val textView = findViewById<TextView>(R.id.error)

        val jsonArrayRequest = JsonObjectRequest(
            Request.Method.GET, hostURL + url, null,
            Response.Listener { response ->
                try {
                    val products = response.getJSONArray("objects")
                    filteredProducts = concatJSON(filteredProducts, filterProducts(products))
                    if (response.optString("next") != "null")
                        this.getJSON(response.optString("next"))
                    // Calculate mean cubic weight once iterated through all api pages
                    else
                        calcAverageCubicWeight()
                }
                catch (e: JSONException) {
                    textView.text = e.message
                }

            },
            Response.ErrorListener { e ->
                textView.text = e.message
            }
        )
        queue.add(jsonArrayRequest)
    }

    private fun calcAverageCubicWeight() {
        val textView = findViewById<TextView>(R.id.result)
        var average = 0.0
        // Calculate and sum the cubic weights
        for (i in 0 until filteredProducts.length())
            average += calcVolume(filteredProducts.getJSONObject(i)) * convertFactor

        // Finish the mean calculation by dividing w/ the filtered product Array length
        textView.text = (average / filteredProducts.length()).toString()
    }

    // Calculate product volume in cubic metres
    private fun calcVolume(product: JSONObject): Double {
        val size = product.getJSONObject("size")
        return size.getDouble("height") * size.getDouble("length") * size.getDouble("width") / 1000000
    }

    // Concatenate two JSONArrays
    private fun concatJSON(arrayA: JSONArray, arrayB: JSONArray): JSONArray {
        val outArray = JSONArray()
        for (i in 0 until arrayA.length())
            outArray.put(arrayA.optJSONObject(i))
        for (i in 0 until arrayB.length())
            outArray.put(arrayB.optJSONObject(i))
        return outArray
    }
    // Filter out objects that aren't Air Conditioners
    private fun filterProducts(products: JSONArray): JSONArray {
        val filteredArray = JSONArray()
        for (i in 0 until products.length()) {
            val product = products.getJSONObject(i)
            if (product["category"] == "Air Conditioners")
                filteredArray.put(product)
        }
        return filteredArray
    }
}
