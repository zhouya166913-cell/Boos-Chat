import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:9090/api',
);

void main() {
  runApp(const BossChatApp());
}

class BossChatApp extends StatefulWidget {
  const BossChatApp({super.key});

  @override
  State<BossChatApp> createState() => _BossChatAppState();
}

class _BossChatAppState extends State<BossChatApp> {
  String? _token;
  CurrentUser? _user;

  void _handleSignedIn(LoginResult result) {
    setState(() {
      _token = result.token;
      _user = result.user;
    });
  }

  void _handleSignedOut() {
    setState(() {
      _token = null;
      _user = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: '企业管理助手',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2F6BFF)),
        useMaterial3: true,
      ),
      home: _token == null
          ? LoginPage(onSignedIn: _handleSignedIn)
          : HomePage(
              token: _token!,
              initialUser: _user!,
              onSignedOut: _handleSignedOut,
            ),
    );
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.onSignedIn});

  final ValueChanged<LoginResult> onSignedIn;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _usernameController = TextEditingController(text: 'admin');
  final _passwordController = TextEditingController(text: 'Admin@123');
  final _api = AuthApi();
  bool _submitting = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final username = _usernameController.text.trim();
    final password = _passwordController.text;

    if (username.isEmpty || password.isEmpty) {
      _showMessage('请输入账号和密码');
      return;
    }

    setState(() => _submitting = true);
    try {
      final result = await _api.login(username, password);
      if (!mounted) return;
      widget.onSignedIn(result);
    } on ApiException catch (error) {
      _showMessage(error.message);
    } catch (_) {
      _showMessage('登录失败，请检查后端服务');
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FB),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Card(
            margin: const EdgeInsets.all(24),
            elevation: 0,
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '企业管理助手',
                    style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  const Text('Flutter App 登录测试'),
                  const SizedBox(height: 24),
                  TextField(
                    controller: _usernameController,
                    decoration: const InputDecoration(
                      labelText: '账号',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _passwordController,
                    obscureText: true,
                    onSubmitted: (_) => _submit(),
                    decoration: const InputDecoration(
                      labelText: '密码',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: Text(_submitting ? '登录中...' : '登录'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({
    super.key,
    required this.token,
    required this.initialUser,
    required this.onSignedOut,
  });

  final String token;
  final CurrentUser initialUser;
  final VoidCallback onSignedOut;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _api = AuthApi();
  late CurrentUser _user = widget.initialUser;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _refreshCurrentUser();
  }

  Future<void> _refreshCurrentUser() async {
    setState(() => _loading = true);
    try {
      final user = await _api.me(widget.token);
      if (!mounted) return;
      setState(() => _user = user);
    } catch (_) {
      if (mounted) {
        widget.onSignedOut();
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _logout() async {
    try {
      await _api.logout(widget.token);
    } finally {
      widget.onSignedOut();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('首页')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '欢迎回来，${_user.displayName}',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 12),
            Text('账号：${_user.username}'),
            Text('角色：${_user.role}'),
            const SizedBox(height: 12),
            Text(_loading ? '正在同步当前用户...' : '后端接口连接正常'),
            const Spacer(),
            FilledButton.tonal(
              onPressed: _logout,
              child: const Text('退出登录'),
            ),
          ],
        ),
      ),
    );
  }
}

class AuthApi {
  Future<LoginResult> login(String username, String password) async {
    final response = await http.post(
      Uri.parse('$apiBaseUrl/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'username': username, 'password': password}),
    );

    final payload = _decodeBody(response);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(payload['message']?.toString() ?? '账号或密码错误');
    }

    return LoginResult.fromJson(payload);
  }

  Future<CurrentUser> me(String token) async {
    final response = await http.get(
      Uri.parse('$apiBaseUrl/auth/me'),
      headers: {'satoken': token},
    );

    final payload = _decodeBody(response);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(payload['message']?.toString() ?? '登录已失效');
    }

    return CurrentUser.fromJson(payload);
  }

  Future<void> logout(String token) async {
    await http.post(
      Uri.parse('$apiBaseUrl/auth/logout'),
      headers: {'satoken': token},
    );
  }

  Map<String, dynamic> _decodeBody(http.Response response) {
    if (response.body.isEmpty) {
      return <String, dynamic>{};
    }
    return jsonDecode(response.body) as Map<String, dynamic>;
  }
}

class LoginResult {
  const LoginResult({required this.token, required this.user});

  final String token;
  final CurrentUser user;

  factory LoginResult.fromJson(Map<String, dynamic> json) {
    return LoginResult(
      token: json['token'] as String,
      user: CurrentUser.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}

class CurrentUser {
  const CurrentUser({
    required this.id,
    required this.username,
    required this.displayName,
    required this.role,
  });

  final int id;
  final String username;
  final String displayName;
  final String role;

  factory CurrentUser.fromJson(Map<String, dynamic> json) {
    return CurrentUser(
      id: json['id'] as int,
      username: json['username'] as String,
      displayName: json['displayName'] as String,
      role: json['role'] as String,
    );
  }
}

class ApiException implements Exception {
  const ApiException(this.message);

  final String message;
}