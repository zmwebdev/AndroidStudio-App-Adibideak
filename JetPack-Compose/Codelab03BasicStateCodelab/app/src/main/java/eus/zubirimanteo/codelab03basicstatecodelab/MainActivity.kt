package eus.zubirimanteo.codelab03basicstatecodelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eus.zubirimanteo.codelab03basicstatecodelab.ui.theme.Codelab03BasicStateCodelabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Codelab03BasicStateCodelabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WellnessScreen()
                }
            }
        }
    }
}


@Composable
fun WaterCounter(modifier: Modifier = Modifier) {
    val count = 0
    Text(
        text = "Zuk $count ur baso dituzu",
        modifier = modifier.padding(16.dp)
    )
}

@Composable
fun WellnessScreen(modifier: Modifier = Modifier) {
    WaterCounter(modifier)
}

/*********************/

@Preview(showBackground = true)
@Composable
fun WaterCounterPreview() {
    Codelab03BasicStateCodelabTheme() { WaterCounter() }
}