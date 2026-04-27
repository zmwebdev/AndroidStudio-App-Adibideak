package eus.zubirimanteo.codelab03basicstatecodelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eus.zubirimanteo.codelab03basicstatecodelab.ui.theme.Codelab03BasicStateCodelabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Codelab03BasicStateCodelabTheme {
                Scaffold { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WellnessScreen()
                    }
                }
            }
        }
    }
}


/*
@Composable
fun WaterCounter(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp))
    {
        //var count : MutableState<Int> = mutableStateOf(0)
        var count by rememberSaveable { mutableStateOf(0) }
        // Gemini("by" azalpena): by gako-hitzak Kotlin-i esaten dio: "Ados, count izeneko aldagai bat definituko dut. Baina bere balioa lortu (get) edo ezarri (set) nahi dudan bakoitzean, ez dut zuzenean kudeatuko. Horren ordez, lana rememberSaveable { mutableStateOf(0) }-k sortutako objektuari pasako diot".
        if (count > 0) {
            var showTask by remember { mutableStateOf(true) }
            if (showTask) {
                WellnessTaskItem(
                    onClose = { showTask = false },
                    taskName = "Gaur zure 15 minutuko ibilaldia egin duzu?"
                )
            }
            Text(text = "Zuk $count ur baso dituzu")
        }

        Row {
            Button(
                onClick = { count++ },
                Modifier.padding(top = 8.dp),
                enabled = count < 10
            ) {
                Text(text = "Bat gehitu")
            }
            Button(
                onClick = { count = 0 },
                Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Text("Garbitu ur kontagailua")
            }
        }
    }
}
*/

@Composable
fun WaterCounter(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        var count by remember { mutableStateOf(0) }
        if (count > 0) {
            Text("You've had $count glasses.")
        }
        Button(onClick = { count++ }, Modifier.padding(top = 8.dp), enabled = count < 10) {
            Text("Add one")
        }
    }
}


@Composable
fun WellnessScreen(modifier: Modifier = Modifier) {
    WaterCounter(modifier)
}

@Composable
fun WellnessTaskItem(
    taskName: String, onClose: () -> Unit, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp), text = taskName
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

/*********************/

@Preview(showBackground = true)
@Composable
fun WaterCounterPreview() {
    Codelab03BasicStateCodelabTheme() { WaterCounter() }
}

// create the Preview of the WellnessTaskItem
@Preview(showBackground = true)
@Composable
fun WellnessTaskItemPreview() {
    WellnessTaskItem(taskName = "Water me", onClose = { })
}
