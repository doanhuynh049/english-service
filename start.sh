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
echo "🌐 Manual trigger available at: http://localhost:$APP_PORT/api/vocabulary/trigger-daily"
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

    # Stop the Maven process gracefully
    if [ ! -z "$MAVEN_PID" ] && kill -0 $MAVEN_PID 2>/dev/null; then
        echo "🔪 Stopping Maven process (PID: $MAVEN_PID)..."
        kill -TERM $MAVEN_PID 2>/dev/null
        
        # Give Maven some time to shut down gracefully
        local timeout=5
        while [ $timeout -gt 0 ] && kill -0 $MAVEN_PID 2>/dev/null; do
            sleep 1
            ((timeout--))
        done
        
        # Force kill Maven if still running
        if kill -0 $MAVEN_PID 2>/dev/null; then
            echo "🔪 Force stopping Maven process..."
            kill -9 $MAVEN_PID 2>/dev/null
        fi
    fi

    # Also clean up any remaining Java processes for this application
    JAVA_PID=$(ps aux | grep "englishService" | grep -v grep | awk '{print $2}')
    if [ ! -z "$JAVA_PID" ]; then
        echo "🔪 Stopping Java application (PID: $JAVA_PID)..."
        kill -TERM $JAVA_PID 2>/dev/null
        sleep 2

        # Force kill if still running
        if kill -0 $JAVA_PID 2>/dev/null; then
            echo "🔪 Force stopping Java application..."
            kill -9 $JAVA_PID 2>/dev/null
        fi
    fi

    # Clean up any remaining processes on the configured port
    REMAINING_PID=$(lsof -ti:$APP_PORT 2>/dev/null)
    if [ ! -z "$REMAINING_PID" ]; then
        echo "🔪 Cleaning up remaining processes on port $APP_PORT..."
        kill -9 $REMAINING_PID 2>/dev/null
    fi

    echo "✅ English Vocabulary Service stopped successfully"
    echo "📚 Thank you for learning English vocabulary! Keep up the great work! 🌟"
    exit 0
}

# Set up signal handlers for graceful shutdown
trap cleanup SIGINT SIGTERM

# Start the application in the background
echo "🚀 Launching English Vocabulary Service..."
mvn spring-boot:run &

# Get the Maven process ID
MAVEN_PID=$!

# Wait for the application to start
echo "⏳ Waiting for application to start..."
sleep 8

# Check if the application started successfully
if ! kill -0 $MAVEN_PID 2>/dev/null; then
    echo "❌ Application failed to start. Check logs for errors."
    exit 1
fi

# Check if the port is actually listening
sleep 2
if ! lsof -i:$APP_PORT >/dev/null 2>&1; then
    echo "⚠️  Application may not be listening on port $APP_PORT yet. Please wait a moment..."
    sleep 3
    if ! lsof -i:$APP_PORT >/dev/null 2>&1; then
        echo "❌ Application is not listening on port $APP_PORT. Check configuration."
    else
        echo "✅ Application is now listening on port $APP_PORT"
    fi
else
    echo "✅ Application is listening on port $APP_PORT"
fi

echo ""
echo "🎉 English Vocabulary Service is now running!"
echo "🌐 Access the service at: http://localhost:$APP_PORT"
echo "📚 API endpoints:"
echo "   • POST /api/vocabulary/trigger-daily - Trigger daily vocabulary"
echo "   • POST /api/vocabulary/process-words-with-email - Process specific words with email"
echo "   • GET  /api/vocabulary/health - Health check"
echo ""
echo "Press Ctrl+C to stop the service gracefully"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Keep the script running and wait for signals
wait $MAVEN_PID 2>/dev/null

# If we reach here, Maven has exited
echo "📝 Maven process has completed"
