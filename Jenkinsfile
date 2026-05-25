pipeline {
  agent any

  parameters {
    string(name: 'DEPLOY_DIR', defaultValue: '/opt/boss-chat', description: 'Local deployment directory on this server')
    string(name: 'SERVICE_NAME', defaultValue: 'boss-chat', description: 'systemd service name')
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

          mkdir -p "$APP_DIR" "$WEB_DIR" "$BACKUP_DIR"

          if [ -f "$APP_DIR/$JAR_NAME" ]; then
            cp "$APP_DIR/$JAR_NAME" "$BACKUP_DIR/$JAR_NAME.$(date +%Y%m%d%H%M%S)"
          fi

          cp "boss-chat-server/target/$JAR_NAME" "$APP_DIR/$JAR_NAME"
          rm -rf "$WEB_DIR"/*
          tar -xzf "$WEB_TAR" -C "$WEB_DIR"

          sudo systemctl restart "$SERVICE_NAME"
          sudo systemctl status "$SERVICE_NAME" --no-pager
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
