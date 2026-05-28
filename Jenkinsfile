pipeline {
  agent any

  parameters {
    string(name: 'DEPLOY_DIR', defaultValue: '/opt/boss-chat', description: 'Local deployment directory on this server')
    string(name: 'SERVICE_NAME', defaultValue: 'boss-chat', description: 'systemd service name')
    string(name: 'HEALTH_URL', defaultValue: 'http://127.0.0.1:9090/api/health', description: 'Local backend health check URL')
  }

  environment {
    JAR_NAME = 'boss-chat-server-0.1.0.jar'
    WEB_TAR = 'boss-chat-web-dist.tar.gz'
  }

  stages {
    stage('Build Backend') {
      steps {
        dir('boss-chat-server') {
          sh 'mvn clean package -DskipTests'
        }
      }
    }

    stage('Build Admin Web') {
      steps {
        dir('boss-chat-web') {
          sh 'npm ci'
          sh 'npm run build'
        }
      }
    }

    stage('Pack Admin Web') {
      steps {
        sh 'tar -czf boss-chat-web-dist.tar.gz -C boss-chat-web/dist .'
      }
    }

    stage('Deploy Locally') {
      steps {
        sh '''
          set -e

          DEPLOY_DIR="${DEPLOY_DIR%/}"
          APP_DIR="$DEPLOY_DIR/app"
          WEB_DIR="$DEPLOY_DIR/web"
          BACKUP_DIR="$DEPLOY_DIR/backup"
          CONFIG_DIR="$DEPLOY_DIR/config"
          UPLOAD_DIR="$DEPLOY_DIR/uploads"

          mkdir -p "$APP_DIR" "$WEB_DIR" "$BACKUP_DIR" "$CONFIG_DIR" "$UPLOAD_DIR"

          if [ -f "$APP_DIR/$JAR_NAME" ]; then
            cp "$APP_DIR/$JAR_NAME" "$BACKUP_DIR/$JAR_NAME.$(date +%Y%m%d%H%M%S)"
          fi

          cp "boss-chat-server/target/$JAR_NAME" "$APP_DIR/$JAR_NAME"
          rm -rf "$WEB_DIR"/*
          tar -xzf "$WEB_TAR" -C "$WEB_DIR"

          sudo systemctl restart "$SERVICE_NAME"
          sudo systemctl status "$SERVICE_NAME" --no-pager || true

          if ! command -v curl >/dev/null 2>&1; then
            echo "curl is required for the Jenkins health check. Please install curl on the server."
            exit 1
          fi

          echo "Waiting for backend health check: $HEALTH_URL"
          for i in $(seq 1 30); do
            if curl -fsS "$HEALTH_URL"; then
              echo
              echo "Backend health check passed."
              exit 0
            fi
            echo "Backend is not ready yet, retry $i/30..."
            sleep 2
          done

          echo "Backend health check failed. Recent service logs:"
          journalctl -u "$SERVICE_NAME" -n 100 --no-pager || true
          exit 1
        '''
      }
    }
  }

  post {
    success {
      echo 'Local deploy completed.'
    }
    failure {
      echo 'Local deploy failed. Check Jenkins console output and server logs.'
    }
  }
}
