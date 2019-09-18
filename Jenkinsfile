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
        volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
        containers: [
                containerTemplate(name: 'maven',
                        image: '779921734503.dkr.ecr.eu-west-1.amazonaws.com/docker-jnlp-slave-image:INFRA',
                        ttyEnabled: true, alwaysPullImage: true, command: 'cat')
        ]) {
    node(POD_LABEL) {
        stage('Bootstrap') {
            sh "echo ${env.BRANCH_NAME}"
        }
        stage('Build a Maven project') {
            git branch: "${env.BRANCH_NAME}", url: 'https://github.com/gchq/Palisade-services.git'
            container('maven') {
                configFileProvider(
                        [configFile(fileId: '450d38e2-db65-4601-8be0-8621455e93b5', variable: 'MAVEN_SETTINGS')]) {
                    sh 'aws s3 ls'
                    sh 'aws ecr list-images --repository-name palisade'
                    sh 'mvn -s $MAVEN_SETTINGS deploy'
                }
            }
        }
    }
}
