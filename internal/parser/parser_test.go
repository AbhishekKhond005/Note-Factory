package parser

import (
	"fmt"
	"testing"
)

func TestParse(t *testing.T) {
	input := `java-roadmap/
├── 00-start-here/
│   ├── how-java-runs/
│   │   ├── source-code → bytecode → JVM
│   │   ├── JDK vs JRE vs JVM
│   │   ├── javac, java, jar
│   │   └── classpath and packagepath
│   ├── language-basics/
│   │   ├── variables and data types
│   │   ├── operators
│   │   ├── type casting
│   │   ├── input/output
│   │   └── comments and formatting
│   └── first-programs/
│       ├── hello world
│       ├── calculator
│       ├── condition checker
│       └── loops practice
`

	rm, err := Parse(input)
	if err != nil {
		t.Fatalf("Parse failed: %v", err)
	}

	if rm.Title != "java-roadmap" {
		t.Errorf("Title = %q, want %q", rm.Title, "java-roadmap")
	}

	if len(rm.Chapters) != 1 {
		t.Fatalf("Expected 1 chapter, got %d", len(rm.Chapters))
	}

	ch := rm.Chapters[0]
	if ch.Name != "00-start-here" {
		t.Errorf("Chapter name = %q, want %q", ch.Name, "00-start-here")
	}

	if len(ch.SubChapters) != 3 {
		t.Fatalf("Expected 3 sub-chapters, got %d", len(ch.SubChapters))
	}

	sub := ch.SubChapters[0]
	if sub.Name != "how-java-runs" {
		t.Errorf("Sub-chapter name = %q, want %q", sub.Name, "how-java-runs")
	}

	if len(sub.Topics) != 4 {
		t.Errorf("Expected 4 topics, got %d: %v", len(sub.Topics), sub.Topics)
	}

	// Print for debugging
	fmt.Printf("Title: %q\n", rm.Title)
	fmt.Printf("Chapter: %q\n", ch.Name)
	for _, s := range ch.SubChapters {
		fmt.Printf("  Sub: %q (topics: %d)\n", s.Name, len(s.Topics))
		for _, t := range s.Topics {
			fmt.Printf("    Topic: %q\n", t)
		}
	}
}


