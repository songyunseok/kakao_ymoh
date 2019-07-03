오유미 [카카오뱅크] 코딩테스트
=======================

# 1. 개발 및 테스트 시현 환경
   - Java 1.8
   - IntelliJ IDEA CE (2018.3.2)
   - Maven 3

# 2. Maven 환경 설정
##   IntelliJ IDEA의 Preference에 Maven User settings file 경로 "~/.m2/settings.xml"와  Local repository "~/.m2/repository"를 별도로 지정하고 다음과 같이 settings.xml을 작성

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

# 3. GIT Clone (예제)
<pre><code>git clone https://github.com/spark3dev/kakao_ymoh.git</code></pre>

# 4. Maven build (예제)
<pre><code>
cd ${user.dir}/${project.home}
mvn -s ${user.dir}/m2/settings.xml -Dmaven.repo.local=${user.dir}/m2/repository clean install
</code></pre>
