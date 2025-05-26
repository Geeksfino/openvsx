#!/bin/bash

# Start Local Authentication for OpenVSX
# This script starts both the mock OAuth2 server and OpenVSX with local authentication

set -e

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
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "❌ Port $port is already in use"
        return 1
    fi
    return 0
}

# Check required ports
echo "🔍 Checking required ports..."
check_port 9999 || exit 1
check_port 8080 || exit 1

echo "✅ Ports are available"

# Start mock OAuth2 server in background
echo "🔐 Starting Mock OAuth2 Server on port 9999..."
cd mock-oauth2-server
./mvnw spring-boot:run > ../mock-oauth2-server.log 2>&1 &
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

# Start OpenVSX server
echo "🌐 Starting OpenVSX Server on port 8080..."
cd server
./gradlew bootRun --args='--spring.profiles.active=local-auth' > ../openvsx-server.log 2>&1 &
OPENVSX_PID=$!
cd ..

# Wait for OpenVSX to start
echo "⏳ Waiting for OpenVSX Server to start..."
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
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
echo "  kill $MOCK_SERVER_PID $OPENVSX_PID"
echo ""

# Keep script running and handle cleanup
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    kill $MOCK_SERVER_PID $OPENVSX_PID 2>/dev/null || true
    echo "✅ Services stopped"
}

trap cleanup EXIT INT TERM

# Wait for user to stop
echo "Press Ctrl+C to stop all services"
wait