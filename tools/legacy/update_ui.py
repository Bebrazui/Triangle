path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_state = '''    var wifiPasswordInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }'''

new_state = '''    var wifiPasswordInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var wifiStatusFeedback by remember { mutableStateOf<String?>(null) }'''

start_marker = '    if (selectedWifiItem != null) {'
end_marker = '    SubScreenScaffold(title = "Сети Wi-Fi", onBack = onBack) {'

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx != -1 and end_idx != -1:
    new_dialog = '''    if (selectedWifiItem != null) {
        val item = selectedWifiItem!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                if (!isConnecting) {
                    selectedWifiItem = null
                    wifiPasswordInput = ""
                    showPassword = false
                    wifiStatusFeedback = null
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Title Header with Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(BorderColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Phosphor.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.ssid,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (item.isSecure) "Защищенная сеть Wi-Fi" else "Открытая сеть",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (item.isSecure) {
                        Text(
                            text = "Пароль",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wifiPasswordInput,
                            onValueChange = {
                                wifiPasswordInput = it
                                wifiStatusFeedback = null
                            },
                            placeholder = { Text("Введите не менее 8 символов", color = TextSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            enabled = !isConnecting,
                            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Phosphor.EyeSlash else Phosphor.Eye,
                                        contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                                        modifier = Modifier.size(20.dp),
                                        tint = TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BorderBold,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BorderBold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Длина: ${wifiPasswordInput.length} символов",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (wifiPasswordInput.isNotEmpty() && wifiPasswordInput.length < 8) AccentTerracotta else TextSecondary
                            )
                            if (wifiPasswordInput.isNotEmpty() && wifiPasswordInput.length < 8) {
                                Text(
                                    text = "Минимум 8",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = AccentTerracotta
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Эта сеть не требует ввода пароля. Подключение происходит напрямую.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    if (wifiStatusFeedback != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (wifiStatusFeedback!!.startsWith("Ошибка") || wifiStatusFeedback!!.startsWith("Не удалось")) {
                                AccentTerracotta.copy(alpha = 0.12f)
                            } else {
                                BorderColor.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = wifiStatusFeedback!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (wifiStatusFeedback!!.startsWith("Ошибка") || wifiStatusFeedback!!.startsWith("Не удалось")) AccentTerracotta else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = !isConnecting,
                            onClick = {
                                selectedWifiItem = null
                                wifiPasswordInput = ""
                                showPassword = false
                                wifiStatusFeedback = null
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Отмена", color = TextSecondary, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            enabled = !isConnecting && (!item.isSecure || wifiPasswordInput.length >= 8),
                            onClick = {
                                isConnecting = true
                                wifiStatusFeedback = "Подключение..."
                                coroutineScope.launch {
                                    val (success, message) = withContext(Dispatchers.IO) {
                                        systemManager.connectToWifi(
                                            item.ssid,
                                            if (item.isSecure) wifiPasswordInput else null
                                        )
                                    }
                                    isConnecting = false
                                    wifiStatusFeedback = message
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        kotlinx.coroutines.delay(1200)
                                        selectedWifiItem = null
                                        wifiPasswordInput = ""
                                        showPassword = false
                                        wifiStatusFeedback = null
                                        refreshList()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BorderBold,
                                contentColor = SurfaceCard,
                                disabledContainerColor = BorderColor.copy(alpha = 0.5f),
                                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SurfaceCard
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Связь...", color = SurfaceCard)
                            } else {
                                Text("Подключить", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
'''

    content = content.replace(old_state, new_state)
    content = content[:start_idx] + new_dialog + '\n' + content[end_idx:]
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('SUCCESS')
else:
    print('DIALOG NOT FOUND', start_idx, end_idx)
