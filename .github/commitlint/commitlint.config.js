// Pull request titles become the squash commit on master.
// Types and scopes are the fixed lists below. A breaking change uses `!`
// and a BREAKING CHANGE footer. The body carries a Refs footer naming the card.

const types = [
    "feat",
    "fix",
    "refactor",
    "test",
    "docs",
    "chore",
    "ci",
    "build",
    "perf",
];

const scopes = [
    "3p-bom",
    "api",
    "bom",
    "changelog",
    "deploy",
    "docs",
    "jakarta-bom",
    "jpa",
    "model",
    "mqtt",
    "mqtt-api",
    "mqtt-impl",
    "mqtt-contract",
    "quarkus",
    "release",
    "ui",
    "websocket",
];

const card = /^(?:[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+)?#\d+$/;

module.exports = {
    extends: ["@commitlint/config-conventional"],
    plugins: [
        {
            rules: {
                "empty-scope": (parsed) => {
                    const header = parsed.header || "";
                    if (/\(\s*\)/.test(header)) {
                        return [false, "scope parentheses must name a scope from the config"];
                    }
                    return [true, ""];
                },
                "refs-card": (parsed) => {
                    const values = [];
                    for (const line of (parsed.raw || "").split(/\r?\n/)) {
                        const match = /^Refs:[ \t]+(\S+)[ \t]*$/.exec(line);
                        if (match) {
                            values.push(match[1]);
                        }
                    }
                    if (values.length === 0 || !values.every((value) => card.test(value))) {
                        return [
                            false,
                            'the body needs a Refs footer naming the card, for example "Refs: #45"',
                        ];
                    }
                    return [true, ""];
                },
            },
        },
    ],
    rules: {
        "type-enum": [2, "always", types],
        "scope-enum": [2, "always", scopes],
        "header-max-length": [0],
        "body-max-line-length": [0],
        "footer-max-line-length": [0],
        "subject-case": [0],
        "breaking-change-exclamation-mark": [2, "always"],
        "trailer-exists": [2, "always", "Refs:"],
        "refs-card": [2, "always"],
        "empty-scope": [2, "always"],
    },
};
