#!/bin/sh
set -eu

REPO="plaaxer/dlq-surgeon"
BIN_NAME="dlq-surgeon"
DEFAULT_PREFIX="${HOME}/.local/bin"

PREFIX="${DEFAULT_PREFIX}"
VERSION=""

usage() {
    cat <<EOF
Install ${BIN_NAME} locally.

Usage: install.sh [--prefix <dir>] [--version <tag>]

Options:
  --prefix <dir>    Install directory (default: ${DEFAULT_PREFIX})
  --version <tag>   Release tag, e.g. v1.2.0 (default: latest)
  -h, --help        Show this help
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --prefix)  PREFIX="$2"; shift 2 ;;
        --version) VERSION="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done

os="$(uname -s)"
arch="$(uname -m)"
if [ "$os" != "Linux" ] || [ "$arch" != "x86_64" ]; then
    echo "error: only Linux x86_64 is supported right now (got ${os} ${arch})" >&2
    echo "       use the fat jar from the GitHub release as a fallback" >&2
    exit 1
fi

if [ -n "$VERSION" ]; then
    url="https://github.com/${REPO}/releases/download/${VERSION}/${BIN_NAME}"
else
    url="https://github.com/${REPO}/releases/latest/download/${BIN_NAME}"
fi

mkdir -p "$PREFIX"
dest="${PREFIX}/${BIN_NAME}"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

echo "downloading ${url}"
if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$url" -o "$tmp"
elif command -v wget >/dev/null 2>&1; then
    wget -q "$url" -O "$tmp"
else
    echo "error: need curl or wget" >&2
    exit 1
fi

chmod +x "$tmp"
mv "$tmp" "$dest"
trap - EXIT

echo "installed ${dest}"

case ":${PATH}:" in
    *":${PREFIX}:"*) ;;
    *)
        echo
        echo "warning: ${PREFIX} is not on \$PATH"
        echo "  add this to your ~/.bashrc or ~/.zshrc:"
        echo "    export PATH=\"${PREFIX}:\$PATH\""
        ;;
esac