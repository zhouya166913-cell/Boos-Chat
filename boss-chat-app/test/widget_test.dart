import 'package:flutter_test/flutter_test.dart';

import 'package:boss_chat_app/main.dart';

void main() {
  testWidgets('shows login form on startup', (WidgetTester tester) async {
    await tester.pumpWidget(const BossChatApp());

    expect(find.text('企业管理助手'), findsOneWidget);
    expect(find.text('Flutter App 登录测试'), findsOneWidget);
    expect(find.text('登录'), findsOneWidget);
  });
}
