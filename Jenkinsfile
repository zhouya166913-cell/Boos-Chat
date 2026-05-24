pipeline {
  agent any

  parameters {
    string(name: 'DEPLOY_HOST', defaultValue: '', description: 'Aliyun ECS public IP or domain')
    string(name: 'DEPLOY_USER', defaultValue: 'deploy', description: 'SSH deploy user')
    string(name: 'DEPLOY_PORT', defaultValue: '22', description: 'SSH port')
    string(name: 'DEPLOY_DIR', defaultValue: '/opt/boss-chat', description: 'Remote deployment directory')
    string(name: 'SERVICE_NAME', defaultValue: 'boss-chat', description: 'systemd service name')
  }

  environment {
    JAR_NAME = 'boss-chat-server-0.1.0.jar'
    WEB_TAR = 'boss-chat-web-dist.tar.gz'
    SSH_CREDENTIALS_ID = 'boss-chat-aliyun-ssh'
  }

  stages {
    stage('Check Parameters') {
      steps {
        script {
          if (!params.DEPLOY_HOST?.trim()) {
            error('DEPLOY_HOST is required')
          }
          if (!params.DEPLOY_USER?.trim()) {
            error('DEPLOY_USER is required')
          }
        }
      }
    }

    stage('Build Backend') {
      steps {
        dir('boss-chat-server') {
          batOrSh('mvn clean package -DskipTests')
        }
      }
    }

    stage('Build Admin Web') {
      steps {
        dir('boss-chat-web') {
          batOrSh('npm ci')
          batOrSh('npm run build')
        }
      }
    }

    stage('Pack Admin Web') {
      steps {
        batOrSh('tar -czf boss-chat-web-dist.tar.gz -C boss-chat-web/dist .')
      }
    }

    stage('Deploy to Aliyun ECS') {
      steps {
        sshagent(credentials: [env.SSH_CREDENTIALS_ID]) {
          batOrSh("""
            ssh -o StrictHostKeyChecking=no -p ${params.DEPLOY_PORT} ${params.DEPLOY_USER}@${params.DEPLOY_HOST} "mkdir -p '${params.DEPLOY_DIR}/app' '${params.DEPLOY_DIR}/web' '${params.DEPLOY_DIR}/backup'"

            scp -P ${params.DEPLOY_PORT} boss-chat-server/target/${env.JAR_NAME} ${params.DEPLOY_USER}@${params.DEPLOY_HOST}:/tmp/${env.JAR_NAME}
            scp -P ${params.DEPLOY_PORT} ${env.WEB_TAR} ${params.DEPLOY_USER}@${params.DEPLOY_HOST}:/tmp/${env.WEB_TAR}

            ssh -p ${params.DEPLOY_PORT} ${params.DEPLOY_USER}@${params.DEPLOY_HOST} "set -e
              if [ -f '${params.DEPLOY_DIR}/app/${env.JAR_NAME}' ]; then cp '${params.DEPLOY_DIR}/app/${env.JAR_NAME}' '${params.DEPLOY_DIR}/backup/${env.JAR_NAME}.\$(date +%Y%m%d%H%M%S)'; fi
              cp '/tmp/${env.JAR_NAME}' '${params.DEPLOY_DIR}/app/${env.JAR_NAME}'
              rm -rf '${params.DEPLOY_DIR}/web'/*
              tar -xzf /tmp/${env.WEB_TAR} -C '${params.DEPLOY_DIR}/web'
              rm -f '/tmp/${env.JAR_NAME}' /tmp/${env.WEB_TAR}
              sudo systemctl restart ${params.SERVICE_NAME}
              sudo systemctl status ${params.SERVICE_NAME} --no-pager"
          """)
        }
      }
    }
  }

  post {
    success {
      echo 'Deploy completed.'
    }
    failure {
      echo 'Deploy failed. Check Jenkins console output and server logs.'
    }
  }
}

def batOrSh(String command) {
  if (isUnix()) {
    sh command
  } else {
    bat command
  }
}
