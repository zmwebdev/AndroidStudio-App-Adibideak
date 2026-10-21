package com.example.navigationstepbysteptutorial

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.navigationstepbysteptutorial.ui.theme.NavigationStepByStepTutorialTheme

@Composable
fun HomeScreen(
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(20) { id ->
            ListItem(
                headlineContent = { Text("Item $id") },
                modifier = Modifier.clickable { onItemClick(id) }
            )
        }
    }
}

@Composable
fun DetailScreen(
    itemId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Details for item $itemId", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings")
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onLoginSuccess) { Text("Log in") }
        TextButton(onClick = onRegisterClick) { Text("Create account") }
    }
}

@Composable
fun RegisterScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onDone) { Text("Done") }
    }
}


/**********************************************/

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NavigationStepByStepTutorialTheme {
        HomeScreen(onItemClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    NavigationStepByStepTutorialTheme {
        DetailScreen(
            itemId = 1,
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NavigationStepByStepTutorialTheme {
        SettingsScreen()
    }
}