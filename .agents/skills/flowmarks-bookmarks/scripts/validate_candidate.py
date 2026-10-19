#!/usr/bin/env python3
"""Validate a FlowMarks or BookmarkAI bookmark candidate against a project."""

from __future__ import annotations

import json
import sys
from pathlib import Path


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    raise SystemExit(1)


def parse_items(root: dict) -> tuple[list[dict], str]:
    if "groups" in root:
        if root.get("version", 1) != 1:
            fail(f"不支持的版本：{root.get('version')}")
        groups = root.get("groups")
        if not isinstance(groups, list) or not groups:
            fail("groups 必须是非空数组")
        items: list[dict] = []
        for group in groups:
            if not isinstance(group, dict) or not isinstance(group.get("items"), list):
                fail("每个分组必须包含 items 数组")
            items.extend(group["items"])
        return items, f"{len(groups)} 个分组"

    group_name = root.get("targetGroupName")
    items = root.get("items")
    if not isinstance(group_name, str) or not group_name.strip():
        fail("候选缺少 targetGroupName")
    if not isinstance(items, list):
        fail("候选缺少 items 数组")
    return items, "1 个分组"


def validate(project_root: Path, candidate_path: Path) -> None:
    project_root = project_root.resolve()
    if not project_root.is_dir():
        fail(f"项目目录不存在：{project_root}")
    try:
        root = json.loads(candidate_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(f"JSON 格式错误：{exc}")
    if not isinstance(root, dict):
        fail("JSON 顶层必须是对象")

    items, group_summary = parse_items(root)
    errors: list[str] = []
    checked = 0

    for index, item in enumerate(items, start=1):
        if not isinstance(item, dict):
            errors.append(f"第 {index} 条不是对象")
            continue
        title = str(item.get("title", f"第 {index} 条"))
        file_path = item.get("filePath")
        line = item.get("line")
        expected = item.get("expectedLineText", "")
        if not isinstance(file_path, str) or not file_path.strip():
            errors.append(f"{title}：缺少 filePath")
            continue
        if not isinstance(line, int) or line < 1:
            errors.append(f"{title}：line 必须是从 1 开始的整数")
            continue
        if not isinstance(expected, str):
            errors.append(f"{title}：expectedLineText 必须是字符串")
            continue

        relative = Path(file_path)
        if relative.is_absolute():
            errors.append(f"{title}：必须使用相对路径：{file_path}")
            continue
        target = (project_root / relative).resolve()
        try:
            target.relative_to(project_root)
        except ValueError:
            errors.append(f"{title}：路径越出项目目录：{file_path}")
            continue
        if not target.is_file():
            errors.append(f"{title}：文件不存在：{file_path}")
            continue

        lines = target.read_text(encoding="utf-8").splitlines()
        if line > len(lines):
            errors.append(f"{title}：行号超出文件范围（文件共 {len(lines)} 行）：{line}")
            continue
        actual = lines[line - 1].strip()
        expected_trimmed = expected.strip()
        if not expected_trimmed:
            errors.append(f"{title}：expectedLineText 为空，无法验证锚点")
            continue
        if actual != expected_trimmed:
            errors.append(
                f"{title}：第 {line} 行与 expectedLineText 不一致；实际为：{actual!r}"
            )
            continue
        checked += 1

    if len(items) > 8:
        print(f"WARNING: 当前 {len(items)} 条，超过建议的 8 条；确认是否应拆分工作流。")
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        print(f"FAILED: {checked}/{len(items)} 条通过校验。")
        raise SystemExit(1)

    print(f"OK: {group_summary}，{checked} 条书签全部通过路径和行号校验。")


def main() -> None:
    if len(sys.argv) != 3:
        print(
            "用法：python validate_candidate.py <项目根目录> <候选 JSON>",
            file=sys.stderr,
        )
        raise SystemExit(2)
    validate(Path(sys.argv[1]), Path(sys.argv[2]))


if __name__ == "__main__":
    main()
