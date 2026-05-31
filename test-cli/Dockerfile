FROM gradle:8.2-jdk17

WORKDIR /app

# 安装 SQLite
RUN apt-get update && apt-get install -y sqlite3 libsqlitej-java

# 复制测试文件
COPY test-cli/TestDatabase.kt /app/

# 创建构建脚本
RUN echo '#!/bin/bash' > /app/run_test.sh && \
    echo 'echo "编译并运行测试..."' >> /app/run_test.sh && \
    echo 'kotlinc -include-runtime -d test.jar /app/TestDatabase.kt 2>&1 || echo "需要 SQLite JDBC 驱动"' >> /app/run_test.sh && \
    echo 'if [ -f test.jar ]; then java -jar test.jar; fi' >> /app/run_test.sh && \
    chmod +x /app/run_test.sh

CMD ["/app/run_test.sh"]
