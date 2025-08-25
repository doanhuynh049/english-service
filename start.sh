#!/bin/bash

# English Vocabulary Service Startup Script

echo "📚 Starting English Vocabulary Service..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Set Java environment (adjust path as needed for your system)
if [ -d "/opt/java-17" ]; then
    export JAVA_HOME=/opt/java-17
    export PATH=$JAVA_HOME/bin:$PATH
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "([0-9]+)' | grep -oP '[0-9]+')
if [ "$JAVA_VERSION" -lt "17" ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version check passed (Java $JAVA_VERSION)"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

echo "✅ Maven check passed"

# Create logs directory if it doesn't exist
mkdir -p logs
echo "✅ Logs directory created"

# Check if application.properties exists and has required configurations
if [ ! -f "src/main/resources/application.properties" ]; then
    echo "❌ application.properties not found. Please create your configuration file."
    exit 1
fi

echo "✅ Configuration file found"

# Check for required configuration properties
CONFIG_FILE="src/main/resources/application.properties"

# Check Gemini API key
if ! grep -q "app.llm-api-key" "$CONFIG_FILE" || grep -q "app.llm-api-key=YOUR_GEMINI_API_KEY" "$CONFIG_FILE"; then
    echo "⚠️  Please configure your Gemini AI API key in application.properties"
    echo "   Set app.llm-api-key=YOUR_ACTUAL_API_KEY"
fi

# Check email configuration
if ! grep -q "app.smtp-user" "$CONFIG_FILE" || ! grep -q "app.smtp-pass" "$CONFIG_FILE"; then
    echo "⚠️  Please configure your email settings in application.properties"
    echo "   Set app.smtp-user and app.smtp-pass with your Gmail credentials"
fi

echo "✅ Configuration validation completed"

# Build the application
echo "🔨 Building English Vocabulary Service..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check for compilation errors."
    exit 1
fi

echo "✅ Build successful"

# Get the configured port (default to 8080)
APP_PORT=$(grep "server.port" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
if [ -z "$APP_PORT" ]; then
    APP_PORT=8080
fi

# Kill any existing process on the configured port
echo "🔍 Checking for existing processes on port $APP_PORT..."
EXISTING_PID=$(lsof -ti:$APP_PORT 2>/dev/null)
if [ ! -z "$EXISTING_PID" ]; then
    echo "⚠️  Found existing process(es) on port $APP_PORT: $EXISTING_PID"
    echo "🔪 Killing existing process(es)..."
    kill -9 $EXISTING_PID 2>/dev/null
    sleep 2
    echo "✅ Existing process(es) killed"
else
    echo "✅ No existing processes found on port $APP_PORT"
fi

# Create Excel log file if it doesn't exist
if [ ! -f "vocabulary_log.xlsx" ]; then
    echo "📊 Excel log file will be created on first vocabulary session"
fi

# Display service information
echo ""
echo "🎯 Starting English Vocabulary Service..."
echo "📧 Daily vocabulary emails will be sent at 5:00 AM every day"
echo "🧠 Using Gemini AI for detailed vocabulary explanations"
echo "📊 Vocabulary words will be logged to vocabulary_log.xlsx"
echo "🌐 Manual trigger available at: http://localhost:$APP_PORT/api/vocabulary/trigger"
echo "❤️  Application running on: http://localhost:$APP_PORT"
echo ""
echo "📚 Features:"
echo "   • Daily automated vocabulary sessions with 10 words"
echo "   • AI-powered explanations with pronunciation, examples, and translations"
echo "   • Beautiful HTML email formatting"
echo "   • Persistent Excel logging"
echo "   • Manual testing via REST API"
echo ""
echo "Press Ctrl+C to stop the service"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Function to handle graceful shutdown
cleanup() {
    echo ""
    echo "🛑 Shutting down English Vocabulary Service..."

    # Find the Java process for this application
    JAVA_PID=$(ps aux | grep "englishService" | grep -v grep | awk '{print $2}')
    if [ ! -z "$JAVA_PID" ]; then
        echo "🔪 Stopping application (PID: $JAVA_PID)..."
        kill -TERM $JAVA_PID 2>/dev/null
        sleep 3

        # Force kill if still running
        if kill -0 $JAVA_PID 2>/dev/null; then
            echo "🔪 Force stopping application..."
            kill -9 $JAVA_PID 2>/dev/null
        fi
    fi

    echo "✅ English Vocabulary Service stopped successfully"
    echo "📚 Thank you for learning English vocabulary! Keep up the great work! 🌟"
    exit 0
}

# Set up signal handlers for graceful shutdown
trap cleanup SIGINT SIGTERM

# Start the application
mvn spring-boot:run &

# Get the Maven process ID
MAVEN_PID=$!

# Wait for the application to start
echo "⏳ Waiting for application to start..."
sleep 10

# Check if application started successfully
if curl -s -f http://localhost:$APP_PORT/api/vocabulary/trigger > /dev/null 2>&1; then
    echo "✅ Application started successfully!"
    echo "🎉 English Vocabulary Service is now running and ready to enhance your vocabulary!"
    echo ""
    echo "💡 Quick Test: curl -X POST http://localhost:$APP_PORT/api/vocabulary/trigger"
    echo ""
else
    echo "⚠️  Application may still be starting up or there might be configuration issues."
    echo "   Check the logs above for any error messages."
    echo "   You can test manually with: curl -X POST http://localhost:$APP_PORT/api/vocabulary/trigger"
    echo ""
fi

# Wait for the Maven process to complete
wait $MAVEN_PID
