package com.example.snapstream

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.snapstream.ui.theme.SnapStreamTheme

// --- Data Models ---
data class User(val fullName: String, val email: String, val password: String)

data class Movie(
    val title: String,
    val imageUrl: String,
    val videoUrl: String = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    val description: String = "A happy-go-lucky Ravi meets with an accident that puts him in a coma for 16 years.",
    val year: String = "2024",
    val rating: String = "U/A 13+",
    val duration: String = "2h 21m",
    val genres: String = "Comedy | RomCom | Drama"
)

enum class Screen { LOGIN, REGISTER, MAIN, DETAILS, PLAYER }
enum class HomeTab { HOME, SEARCH, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapStreamTheme {
                var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
                var currentUser by remember { mutableStateOf<User?>(null) }
                val registeredUsers = remember { mutableStateListOf<User>() }
                var selectedMovie by remember { mutableStateOf<Movie?>(null) }

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    when (currentScreen) {
                        Screen.LOGIN -> LoginScreen(
                            registeredUsers = registeredUsers,
                            onLoginSuccess = { user -> currentUser = user; currentScreen = Screen.MAIN },
                            onNavigateToRegister = { currentScreen = Screen.REGISTER }
                        )
                        Screen.REGISTER -> RegisterScreen(
                            onRegisterSuccess = { newUser -> registeredUsers.add(newUser); currentScreen = Screen.LOGIN },
                            onNavigateToLogin = { currentScreen = Screen.LOGIN }
                        )
                        Screen.MAIN -> MainContentWrapper(
                            user = currentUser,
                            onLogout = { currentScreen = Screen.LOGIN },
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                currentScreen = Screen.DETAILS
                            }
                        )
                        Screen.DETAILS -> MovieDetailsScreen(
                            movie = selectedMovie!!,
                            onBack = { currentScreen = Screen.MAIN },
                            onWatchNow = { currentScreen = Screen.PLAYER }
                        )
                        Screen.PLAYER -> VideoPlayerScreen(
                            videoUrl = selectedMovie?.videoUrl ?: "",
                            onBack = { currentScreen = Screen.DETAILS }
                        )
                    }
                }
            }
        }
    }
}

// --- Working Main Content Wrapper ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentWrapper(user: User?, onLogout: () -> Unit, onMovieClick: (Movie) -> Unit) {
    var selectedTab by remember { mutableStateOf(HomeTab.HOME) }

    // Shared movie list for the whole app
    val movies = remember {
        listOf(
            Movie("Comali", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500"),
            Movie("Interstellar", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500"),
            Movie("Inception", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500"),
            Movie("The Batman", "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=500"),
            Movie("Thayee Kilavi", "https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=800")
        )
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color.Black, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.HOME,
                    onClick = { selectedTab = HomeTab.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.SEARCH,
                    onClick = { selectedTab = HomeTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.PROFILE,
                    onClick = { selectedTab = HomeTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                HomeTab.HOME -> HomeScreen(movies, onMovieClick)
                HomeTab.SEARCH -> SearchScreen(movies, onMovieClick)
                HomeTab.PROFILE -> ProfileScreen(user, onLogout)
            }
        }
    }
}

// --- Home Screen ---
@Composable
fun HomeScreen(movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeroBanner(movies.first(), onMovieClick) }
        item { MovieSection("Trending Now", movies, onMovieClick) }
        item { MovieSection("Popular Shows", movies.reversed(), onMovieClick) }
    }
}

@Composable
fun HeroBanner(movie: Movie, onMovieClick: (Movie) -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(450.dp)
        .clickable { onMovieClick(movie) }) {
        AsyncImage(model = movie.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black),
                    startY = 350f
                )
            ))
        Column(modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(20.dp)) {
            Text(movie.title, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Button(onClick = { onMovieClick(movie) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))) {
                Text("View Details")
            }
        }
    }
}

@Composable
fun MovieSection(title: String, movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(movies) { movie ->
                Card(modifier = Modifier
                    .width(160.dp)
                    .height(90.dp)
                    .clickable { onMovieClick(movie) }) {
                    AsyncImage(model = movie.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

// --- WORKING SEARCH SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filteredMovies = remember(query) {
        movies.filter { it.title.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search movies...", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.DarkGray,
                unfocusedContainerColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(filteredMovies) { movie ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onMovieClick(movie) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = movie.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp, 60.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(movie.title, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

// --- WORKING PROFILE SCREEN ---
@Composable
fun ProfileScreen(user: User?, onLogout: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(60.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0083EE), Color(0xFFC71E7C)))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = user?.fullName?.take(1)?.uppercase() ?: "S", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = user?.fullName ?: "SnapStream User", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = user?.email ?: "guest@snapstream.com", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        ProfileMenuItem(Icons.Default.Favorite, "My List") { Toast.makeText(context, "Opening List", Toast.LENGTH_SHORT).show() }
        ProfileMenuItem(Icons.Default.Notifications, "Notifications") { Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show() }
        ProfileMenuItem(Icons.Default.Settings, "App Settings") { }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ExitToApp, null, tint = Color.Red)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(title, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray)
    }
}

// --- Video Player & Movie Details (As before, but complete) ---

@Composable
fun MovieDetailsScreen(movie: Movie, onBack: () -> Unit, onWatchNow: () -> Unit) {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        item {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)) {
                AsyncImage(model = movie.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black),
                            startY = 300f
                        )
                    ))
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 40.dp, start = 10.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Column(modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)) {
                    Text(movie.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Button(onClick = onWatchNow, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), border = BorderStroke(1.dp, Color.Gray)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Text("Watch Now")
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(videoUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(videoUrl))
        prepare()
        playWhenReady = true
    }}
    DisposableEffect(Unit) { onDispose { player.release() } }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 40.dp, start = 16.dp)) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
        }
    }
}

// --- Auth Screens ---
@Composable
fun LoginScreen(registeredUsers: List<User>, onLoginSuccess: (User) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SnapStream", color = Color.Red, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(30.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            val user = registeredUsers.find { it.email == email && it.password == password }
            if (user != null) onLoginSuccess(user)
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("LOGIN") }
        TextButton(onClick = onNavigateToRegister) { Text("Register here", color = Color.Gray) }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: (User) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Register", color = Color.White, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(20.dp))
        TextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { if(fullName.isNotBlank()) onRegisterSuccess(User(fullName, email, password)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("REGISTER") }
        TextButton(onClick = onNavigateToLogin) { Text("Back", color = Color.Gray) }
    }
}