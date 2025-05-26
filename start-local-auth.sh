#!/bin/bash

# Start Local Authentication for OpenVSX
# This script starts both the mock OAuth2 server and OpenVSX with local authentication

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 Starting OpenVSX with Local Authentication"
echo "=============================================="

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed or not in PATH"
    exit 1
fi

# Function to check if port is available
check_port() {
    local port=$1
    # Use a more portable way to check ports since lsof might not be available
    if timeout 1 bash -c "echo >/dev/tcp/localhost/$port" 2>/dev/null; then
        echo "❌ Port $port is already in use"
        return 1
    fi
    return 0
}

# Check required ports
echo "🔍 Checking required ports..."

# Check if Mock OAuth2 server is already running
if ! check_port 9999; then
    echo "ℹ️  Port 9999 is in use, checking if it's our Mock OAuth2 server..."
    if curl -s http://localhost:9999/ > /dev/null 2>&1; then
        echo "✅ Mock OAuth2 Server is already running"
        MOCK_SERVER_PID=""
    else
        echo "ℹ️  Port 9999 is in use by another process, attempting to kill it..."
        pkill -f "mock-oauth2-server" || true
        sleep 2
        MOCK_SERVER_PID=""
    fi
else
    # Start mock OAuth2 server in background
    echo "🔐 Starting Mock OAuth2 Server on port 9999..."
    cd mock-oauth2-server
    # Use the pre-built JAR instead of Maven
    if [ ! -f "target/mock-oauth2-server-1.0.0.jar" ]; then
        echo "❌ Mock OAuth2 Server JAR not found at: $(pwd)/target/mock-oauth2-server-1.0.0.jar"
        echo "Please build the Mock OAuth2 server first with: cd mock-oauth2-server && mvn clean package"
        exit 1
    fi
    java -jar target/mock-oauth2-server-1.0.0.jar > ../mock-oauth2-server.log 2>&1 &
    MOCK_SERVER_PID=$!
    cd ..
    
    # Wait for mock server to start
    echo "⏳ Waiting for Mock OAuth2 Server to start..."
    for i in {1..30}; do
        if curl -s http://localhost:9999/ > /dev/null 2>&1; then
            echo "✅ Mock OAuth2 Server is running"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "❌ Mock OAuth2 Server failed to start"
            kill $MOCK_SERVER_PID 2>/dev/null || true
            exit 1
        fi
        sleep 1
    done
fi

if ! check_port 8080; then
    echo "ℹ️  Port 8080 is in use, OpenVSX server may already be running"
fi

echo "✅ Ports checked"

# Check if OpenVSX is already running
if check_port 8080; then
    # Start OpenVSX server
    echo "🌐 Starting OpenVSX Server on port 8080..."
    cd server
    if [ ! -f "build/libs/openvsx-server.jar" ]; then
        echo "❌ OpenVSX Server JAR not found at: $(pwd)/build/libs/openvsx-server.jar"
        echo "Please build the OpenVSX server first with: ./gradlew build"
        exit 1
    fi
    java -jar build/libs/openvsx-server.jar --spring.profiles.active=local-auth --server.port=8080 > ../openvsx-server.log 2>&1 &
    OPENVSX_PID=$!
    cd ..

    # Wait for OpenVSX to start
    echo "⏳ Waiting for OpenVSX Server to start..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/api/version > /dev/null 2>&1; then
            echo "✅ OpenVSX Server is running"
            break
        fi
        if [ $i -eq 60 ]; then
            echo "❌ OpenVSX Server failed to start"
            kill $MOCK_SERVER_PID $OPENVSX_PID 2>/dev/null || true
            exit 1
        fi
        sleep 2
    done
else
    echo "ℹ️  OpenVSX Server is already running on port 8080"
    OPENVSX_PID=""
fi

echo ""
echo "🎉 Local Authentication Setup Complete!"
echo "======================================="
echo ""
echo "📋 Available Test Users:"
echo "  • admin/admin123 (Admin role)"
echo "  • user/user123 (Regular user)"
echo "  • publisher/pub123 (Privileged user)"
echo "  • testuser/test123 (Regular user)"
echo ""
echo "🌐 Services:"
echo "  • OpenVSX: http://localhost:8080"
echo "  • Mock OAuth2 Server: http://localhost:9999"
echo ""
echo "📝 Logs:"
echo "  • Mock OAuth2 Server: mock-oauth2-server.log"
echo "  • OpenVSX Server: openvsx-server.log"
echo ""
echo "🛑 To stop services:"
PIDS=""
if [ -n "$MOCK_SERVER_PID" ]; then
    PIDS="$PIDS $MOCK_SERVER_PID"
fi
if [ -n "$OPENVSX_PID" ]; then
    PIDS="$PIDS $OPENVSX_PID"
fi
if [ -n "$PIDS" ]; then
    echo "  kill$PIDS"
else
    echo "  Services are already running (use pkill -f 'mock-oauth2-server' or pkill -f 'openvsx-server')"
fi
echo ""

# Keep script running and handle cleanup
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    if [ -n "$MOCK_SERVER_PID" ]; then
        kill $MOCK_SERVER_PID 2>/dev/null || true
    fi
    if [ -n "$OPENVSX_PID" ]; then
        kill $OPENVSX_PID 2>/dev/null || true
    fi
    echo "✅ Services stopped"
}

trap cleanup EXIT INT TERM

# Wait for user to stop
echo "Press Ctrl+C to stop all services"
wait