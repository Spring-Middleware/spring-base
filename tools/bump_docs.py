import os
import re
replacements = [
    (r'<spring-middleware\.api\.version>1\.5\.0</spring-middleware\.api\.version>', r'<spring-middleware.api.version>1.6.0</spring-middleware.api.version>'),
    (r'<spring-middleware\.app\.version>1\.8\.0</spring-middleware\.app\.version>', r'<spring-middleware.app.version>1.9.0</spring-middleware.app.version>'),
    (r'<spring-middleware\.mongo-core\.version>1\.6\.0</spring-middleware\.mongo-core\.version>', r'<spring-middleware.mongo-core.version>1.7.0</spring-middleware.mongo-core.version>'),
    (r'<spring-middleware\.kafka-core\.version>1\.5\.0</spring-middleware\.kafka-core\.version>', r'<spring-middleware.kafka-core.version>1.6.0</spring-middleware.kafka-core.version>'),
    (r'<spring-middleware\.graphql\.version>1\.5\.0</spring-middleware\.graphql\.version>', r'<spring-middleware.graphql.version>1.6.0</spring-middleware.graphql.version>'),
    (r'\*\*Current Version:\*\* `1\.5\.0`', r'**Current Version:** `1.6.0`'),
    (r'<version>1\.5\.0</version>', r'<version>1.6.0</version>'),
    (r'io\.github\.spring-middleware:bom:1\.5\.0', r'io.github.spring-middleware:bom:1.6.0'),
]
property_updates = {
    'core.commons.version': '1.7.0',
    'core.view.version': '1.6.0',
    'core.cache.version': '1.6.0',
    'core.api.version': '1.6.0',
    'core.app.version': '1.9.0',
    'core.mongo.version': '1.7.0',
    'core.rabbitmq.version': '1.6.0',
    'core.kafka.version': '1.6.0',
    'core.jpa.version': '1.7.0',
    'core.redis.version': '1.6.0',
    'core.model.version': '1.6.0',
    'core.registry.version': '1.7.0',
    'core.graphql.version': '1.6.0',
    'core.graphql.gateway.version': '1.5.0',
    'core.ai.version': '1.2.0',
    'core.ai.ollama.version': '1.2.0',
}
def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    original_contcat << 'EOF' > tools/bump_docs.py
import os
import re
replacements = [
    (r'<spring-middleware\.api\.version>1\.5\.0</spring-middleware\.api\.version>', r'<spring-middleware.api.version>1.6.0</spring-middleware.api.version>'),
    (r'<spring-middleware\.app\.version>1\.8\.0</spring-middleware\.app\.version>', r'<spring-middleware.app.version>1.9.0</spring-middleware.app.version>'),
    (r'<spring-middleware\.mongo-core\.version>1\.6\.0</spring-middleware\.mongo-core\.version>', r'<spring-middleware.mongo-core.version>1.7.0</spring-middleware.mongo-core.version>'),
    (r'<spring-middleware\.kafka-core\.version>1\.5\.0</spring-middleware\.kafka-core\.version>', r'<spring-middleware.kafka-core.version>1.6.0</spring-middleware.kafka-core.version>'),
    (r'<spring-middleware\.graphql\.version>1\.5\.0</spring-middleware\.graphql\.version>', r'<spring-middleware.graphql.version>1.6.0</spring-middleware.graphql.version>'),
    (r'\*\*Current Version:\*\* `1\.5\.0`', r'**Current Version:** `1.6.0`'),
    (r'<version>1\.5\.0</version>', r'<version>1.6.0</version>'),
    (r'io\.github\.spring-middleware:bom:1\.5\.0', r'io.github.spring-middleware:bom:1.6.0'),
]
property_updates = {
    'core.commons.version': '1.7.0',
    'core.view.version': '1.6.0',
    'core.cache.version': '1.6.0',
    'core.api.version': '1.6.0',
    'core.app.version': '1.9.0',
    'core.mongo.version': '1.7.0',
    'core.rabbitmq.version': '1.6.0',
    'core.kafka.version': '1.6.0',
    'core.jpa.version': '1.7.0',
    'core.redis.version': '1.6.0',
    'core.model.version': '1.6.0',
    'core.registry.version': '1.7.0',
    'core.graphql.version': '1.6.0',
    'core.graphql.gateway.version': '1.5.0',
    'core.ai.version': '1.2.0',
    'core.ai.ollama.version': '1.2.0',
}
def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    original_content = content
    for old, new in replacements:
        content = re.sub(old, new, content)
    for prop, new_version in property_updates.items():
        # Match either "core.commons.version | 1.X.X |" or "core.commons.version   1.X.X"
        content = re.sub(
            rf'({prop}(\s*\|\s*|\s+))[\d\.]+(\s*\|)?',
            rf'\g<1>{new_version}\g<3>',
            content
        )
    if original_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated docs in: {filepath}")
def main():
    skip_dirs = ['node_modules', 'target', '.git']
    for root, dirs, files in os.walk('.'):
        dirs[:] = [d for d in dirs if d not in skip_dirs]
        for name in files:
            if name.endswith('.md'):
                filepath = os.path.join(root, name)
                process_file(filepath)
if __name__ == '__main__':
    main()
