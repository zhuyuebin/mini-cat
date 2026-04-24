#!/bin/bash

echo "========================================="
echo "  MiniCat - 快速启动脚本"
echo "========================================="
echo ""

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未检测到Java,请先安装JDK 17或更高版本"
    echo "   下载地址: https://adoptium.net/"
    exit 1
fi

# 显示Java版本
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "✓ Java版本: $JAVA_VERSION"

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "⚠️  警告: 未检测到Maven,将使用Maven Wrapper"
    MAVEN_CMD="./mvnw"
else
    MAVEN_CMD="mvn"
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
    echo "✓ Maven版本: $MVN_VERSION"
fi

echo ""
echo "正在构建项目..."
echo ""

# 构建项目
$MAVEN_CMD clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 构建失败!"
    exit 1
fi

echo ""
echo "✓ 构建成功!"
echo ""

# 查找JAR文件
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ 未找到JAR文件!"
    exit 1
fi

echo "========================================="
echo "  启动MiniCat..."
echo "========================================="
echo ""
echo "访问地址: http://localhost:8080"
echo ""
echo "按 Ctrl+C 停止服务"
echo ""

# 启动应用
java -jar "$JAR_FILE"
