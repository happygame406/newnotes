package com.example.newnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.newnotes.ui.theme.New_NotesTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            New_NotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteApp()
                }
            }
        }
    }
}

@Composable
fun NoteApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNoteClick = { noteId -> navController.navigate("detail/$noteId") }
            )
        }
        composable(
            "detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            println("Получен ID заметки: $noteId")
            DetailScreen(noteId = noteId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNoteClick: (Long) -> Unit) {
    val viewModel: NoteViewModel = viewModel()
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var userInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Введите текст") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                println("Текст из поля ввода: $userInput")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800),
                contentColor = Color.White
            ),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Вывести в консоль")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Содержание") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    viewModel.insertNote(title, content)
                    title = ""
                    content = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Добавить заметку")
        }

        Spacer(modifier = Modifier.height(16.dp))

        notes.forEach { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onDeleteClick = {
                    noteToDelete = note
                    showDialog = true
                }
            )
        }
    }

    if (showDialog && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                noteToDelete = null
            },
            title = { Text("Подтверждение удаления") },
            text = { Text("Вы уверены, что хотите удалить эту заметку?") },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete?.let { viewModel.deleteNote(it) }
                    showDialog = false
                    noteToDelete = null
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    noteToDelete = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = note.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dateFormat.format(Date(note.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text("Удалить")
            }
        }
    }
}

@Composable
fun DetailScreen(noteId: Long) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ID заметки: $noteId")
    }
}