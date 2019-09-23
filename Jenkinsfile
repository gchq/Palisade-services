/*
 * Copyright 2019 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
podTemplate(
        name: 'palisade',
        volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
        containers: [
                containerTemplate(name: 'maven',
                        image: "${env.INFRA_IMAGE}",
                        ttyEnabled: true, alwaysPullImage: false, command: 'cat',
                        envVars: [envVar(key: 'TILLER_NAMESPACE', value: 'tiller'), envVar(key: 'HELM_HOST', value: ':44134')],
                        ports: [portMapping(name: 'ryuk', containerPort: 32773, hostPort: 32773)])
        ]) {
    node(POD_LABEL) {
        stage('Bootstrap') {
            echo sh(script: 'env|sort', returnStdout: true)
        }
        stage('Install a Maven project') {
            git branch: "${env.BRANCH_NAME}", url: 'https://github.com/gchq/Palisade-services.git'
            container('maven') {
                configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                    sh 'palisade-login'
                    sh 'helm list'
                    sh 'mvn -s $MAVEN_SETTINGS install'
                }
            }
        }
        stage('Deploy a Maven project') {
            git branch: "${env.BRANCH_NAME}", url: 'https://github.com/gchq/Palisade-services.git'
            container('maven') {
                configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                    sh 'palisade-login'
                    sh 'helm list'
                    sh 'mvn -s $MAVEN_SETTINGS deploy'
                }
            }
        }
    }
}




