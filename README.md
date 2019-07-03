오유미 [카카오뱅크] 코딩테스트
=======================
# 1. Project 구성
   - FileBuffer: 공통 Protobuf Model 및 SSL, Java NIO Server Base, Utility 클래스 제공
   - FileGateway: 가상 외부 시스템과 파일 송수신을 처리하는 Gateway Server (Spring Boot Console)
      - Pull: 외부 시스템으로 부터 파일 수신
      - Push: 외부 시스템에 파일 송신
   - FileTransfer: 가상 내부 시스템과 Gateway 간의 파일 송수신을 처리하는 Transport Server (Spring Boot Console)
      - Pull: Gateway Server로 부터 파일 수신
      - Push: Gateway Server에 파일 송신
      
# 2. 개발 및 테스트 시현 환경
   - Java 1.8
   - IntelliJ IDEA CE (2018.3.2)
   - Maven 3

# 3. Maven 환경 설정
##   IntelliJ IDEA의 Preference에 Maven User settings file 경로 ```(~/.m2/settings.xml)```와  Local repository ```(~/.m2/repository)```를 별도로 지정하고 다음과 같이 settings.xml을 작성

<pre><code>
&lt;settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd"&gt;
    &lt;profiles&gt;
        &lt;profile&gt;
            &lt;id&gt;securecentral&lt;/id&gt;
            &lt;activation&gt;
                &lt;activeByDefault&gt;true&lt;/activeByDefault&gt;
            &lt;/activation&gt;
            &lt;!--Override the repository (and pluginRepository) "central" from the
               Maven Super POM --&gt;
            &lt;repositories&gt;
                &lt;repository&gt;
                    &lt;id&gt;central&lt;/id&gt;
                    &lt;url&gt;https://repo1.maven.org/maven2&lt;/url&gt;
                    &lt;releases&gt;
                        &lt;enabled&gt;true&lt;/enabled&gt;
                    &lt;/releases&gt;
                &lt;/repository&gt;
            &lt;/repositories&gt;
            &lt;pluginRepositories&gt;
                &lt;pluginRepository&gt;
                    &lt;id&gt;central&lt;/id&gt;
                    &lt;url&gt;https://repo1.maven.org/maven2&lt;/url&gt;
                    &lt;releases&gt;
                        &lt;enabled&gt;true&lt;/enabled&gt;
                    &lt;/releases&gt;
                &lt;/pluginRepository&gt;
            &lt;/pluginRepositories&gt;
        &lt;/profile&gt;
    &lt;/profiles&gt;
&lt;/settings&gt;
</code></pre>

# 4. GIT Clone (예제)
<pre><code>git clone https://github.com/spark3dev/kakao_ymoh.git</code></pre>

# 5. Maven build (예제)
<pre><code>
cd ${user.dir}/${project.home}
mvn -s ${user.dir}/m2/settings.xml -Dmaven.repo.local=${user.dir}/m2/repository clean install
</code></pre>

# 6. Gateway Server (FileGateway) 실행
## 6.1. Spring Boot Application 실행
<pre><code>
cd ${user.dir}/${project.home}/FileGateway
java -jar ./target/FileGateway-1.0.0.jar 
</code></pre>
## 6.2. Spring Shell
   - system status
   - list sessions
   <pre><code>
   shell:&gt;list sessions
   ------------------------------------------------------------------------
   | ID             | ADDRESS                          | STATUS           |
   ------------------------------------------------------------------------
   |FS_2            |/127.0.0.1:62593                  |ACTIVE            |
   ------------------------------------------------------------------------
   |FS_1            |/127.0.0.1:62589                  |ACTIVE            |
   ------------------------------------------------------------------------
   </code></pre>
   - list operations
   <pre><code>
   shell:&gt;list operations
   --------------------------------------------
   | METHOD         | GOOD       | BAD        |
   --------------------------------------------
   |PULL            |           1|           0|
   --------------------------------------------
   |PUSH            |           1|           0|
   --------------------------------------------
   </code></pre>
   - help (more shells)
   - shutdown

# 7. Transport Server (FileTransfer) 실행
## 7.1. Spring Boot Application 실행
<pre><code>
cd ${user.dir}/${project.home}/FileTransfer
java -jar ./target/FileTransfer-1.0.0.jar
</code></pre>
## 7.2. Spring Shell
   - start-pull <i>user</i>
   <pre><code>
   shell:&gt;start-pull super
   </code></pre>
   - start-push <i>user</i>
   <pre><code>
   shell:&gt;start-push super
   </code></pre>
   - list <i>user</i>
   <pre><code>
   shell:&gt;list super
   ---------------------------------------------------------------
   | ID             | METHOD     | STATUS           | COUNT      |
   ---------------------------------------------------------------
   |PULL_1          |PULL        |ACTIVE            |           1|
   ---------------------------------------------------------------
   |PUSH_1          |PUSH        |ACTIVE            |           1|
   ---------------------------------------------------------------
   </code></pre>
   - stop <i>id</i>
   <pre><code>
   shell:&gt;stop PULL_1
   </code></pre>
   - help (more shells)
   - shutdown
   
# 8. TODO
   - SSL/TLS 암호화 통신
   - Hmac-SHA256 Signature 보안 인증
