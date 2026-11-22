# -*- coding: utf-8 -*-
# One-off repair of History.md's tail structure: §574's closing sentence was displaced to the file
# end (after §577) by the chained anchor edits; restore it to §574's end and drop the orphan.

import io

P = "doc/books/AdaptKey-History.md"
ORPHAN = ("Quality gate PASS, real-data test subset green. NOT yet device-confirmed; M9's "
          "regression round\npins the china case.")
SECTION_574_END = ("class rows) still force-cap - the same wrongness shape, discoverable only word "
                   "by word.")


def main():
    with io.open(P, encoding="utf-8", newline="") as f:
        text = f.read()

    # 1) Remove the orphan block at the file end (preceded by blank lines).
    orphan_block = "\n\n\n" + ORPHAN
    assert orphan_block in text, "orphan block not found"
    assert text.rstrip().endswith(ORPHAN), "orphan is not the file's last content"
    text = text[: text.index(orphan_block)] + "\n"

    # 2) Restore the sentence as §574's closing paragraph.
    anchor = SECTION_574_END
    assert text.count(anchor) == 1, text.count(anchor)
    text = text.replace(
        anchor,
        anchor + "\n\n" + ORPHAN,
        1,
    )

    with io.open(P, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    print("History tail repaired: §574 closing sentence restored, orphan removed")


if __name__ == "__main__":
    main()
