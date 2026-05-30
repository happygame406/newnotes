package com.example.newnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
            HomeScreen(onNoteClick = { noteId -> navController.navigate("detail/$noteId") })
        }
        composable(
            "detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            DetailScreen(
                noteId = noteId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// ==================== HOME SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(onNoteClick: (Long) -> Unit) {
    val viewModel: NoteViewModel = viewModel()
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())

    var backgroundColor by remember { mutableStateOf(Color(0xFFF5F5F5)) }
    var cardColor by remember { mutableStateOf(Color(0xFFE3F2FD)) }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var userInput by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }

    val contentTextColor by remember { derivedStateOf {
        if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    }}

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = userInput, onValueChange = { userInput = it },
                label = { Text("Введите текст", color = contentTextColor) },
                textStyle = TextStyle(color = contentTextColor),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { println("Текст из поля ввода: $userInput") }, modifier = Modifier.fillMaxWidth()) {
                Text("Вывести в консоль", color = contentTextColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Цвет темы:", color = contentTextColor)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showColorPicker = true }) { Text("Выбрать цвет") }
            }

            if (showColorPicker) {
                ColorPickerDialog(
                    onColorSelected = { selectedColor ->
                        backgroundColor = selectedColor
                        cardColor = selectedColor.copy(alpha = 0.9f)
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Заголовок", color = contentTextColor) }, textStyle = TextStyle(color = contentTextColor), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Содержание", color = contentTextColor) }, textStyle = TextStyle(color = contentTextColor), modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    viewModel.insertNote(title, content)
                    title = ""; content = ""
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить заметку", color = contentTextColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            notes.forEach { note ->
                NoteCard(note = note, onClick = { onNoteClick(note.id) }, onDeleteClick = { noteToDelete = note; showDialog = true }, cardColor = cardColor)
            }
        }
    }

    if (showDialog && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false; noteToDelete = null },
            title = { Text("Подтверждение") },
            text = { Text("Удалить эту заметку?") },
            confirmButton = { TextButton(onClick = { noteToDelete?.let { viewModel.deleteNote(it) }; showDialog = false; noteToDelete = null }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { showDialog = false; noteToDelete = null }) { Text("Отмена") } }
        )
    }
}

// ==================== DETAIL SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(noteId: Long, onBackClick: () -> Unit) {
    val viewModel: NoteViewModel = viewModel()
    val context = LocalContext.current
    val db = NoteDatabase.getDatabase(context)
    val noteDao = db.noteDao()

    var note by remember { mutableStateOf<Note?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        noteDao.getAllNotes().collect { notesList ->
            note = notesList.find { it.id == noteId }
            note?.let {
                editTitle = it.title
                editContent = it.content
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Редактирование заметки" else "Заметка") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isEditing) {
                        // Кнопка "Сохранить" когда редактируем
                        TextButton(onClick = {
                            note?.let {
                                viewModel.updateNote(it.copy(title = editTitle, content = editContent))
                            }
                            isEditing = false
                        }) {
                            Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Обычное меню
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                onClick = { isEditing = true; showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", color = Color.Red) },
                                onClick = { showDeleteDialog = true; showMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F1F1F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("Содержание") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    maxLines = 20
                )
            } else {
                note?.let { currentNote ->
                    Text(text = currentNote.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = dateFormat.format(Date(currentNote.timestamp)), color = Color.Gray, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = currentNote.content, fontSize = 17.sp, lineHeight = 26.sp, color = Color.White)
                } ?: Text("Заметка не найдена", color = Color.Gray)
            }
        }
    }

    // Диалог удаления
    if (showDeleteDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    note?.let { viewModel.deleteNote(it) }
                    showDeleteDialog = false
                    onBackClick()
                }) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// ==================== ColorPickerDialog и NoteCard ====================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val colors = listOf(
        Color.White, Color.Black, Color(0xFFE3F2FD), Color(0xFFF3E5F5),
        Color(0xFFE8F5E9), Color(0xFFFFF3E0), Color(0xFFFCE4EC),
        Color(0xFFE0F2F1), Color(0xFFF1F8E9), Color(0xFFE8EAF6)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите цвет темы") },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(color, CircleShape)
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDeleteClick: () -> Unit, cardColor: Color) {
    val textColor = if (cardColor.luminance() > 0.5f) Color.Black else Color.White
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = note.title, style = MaterialTheme.typography.titleMedium, color = textColor)
                Text(text = dateFormat.format(Date(note.timestamp)), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.85f))
            }
            Button(onClick = onDeleteClick, colors = ButtonDefaults.buttonColors(Color.Red)) {
                Text("Удалить", color = Color.White)
            }
        }
    }
}