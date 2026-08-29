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
	if ch.Name != "start-here" {
		t.Errorf("Chapter name = %q, want %q", ch.Name, "start-here")
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

// TestParseTwoLevelChapters covers tree roadmaps whose chapters contain two
// levels of granularity, like SpringSecurity.txt: some chapters hold
// sub-chapters with topics, others hold leaf items (.md files) directly,
// and every name carries a numeric order prefix.
func TestParseTwoLevelChapters(t *testing.T) {
	input := `spring-security-roadmap/
│
├── 00-prerequisites/
│   ├── 01-java-web-basics/
│   │   ├── internet-vs-web.md
│   │   ├── client-server.md
│   │   └── request-response.md
│   └── 02-http/
│       ├── http-methods.md
│       └── status-codes.md
│
├── 01-servlet-world/
│   ├── 01-servlet-api.md
│   ├── 02-servlet-container.md
│   └── 03-tomcat.md
│
└── 02-mixed/
    ├── 01-grouped-topic/
    │   ├── confidentiality.md
    │   └── integrity.md
    └── 02-leaf-topic.md
`

	rm, err := Parse(input)
	if err != nil {
		t.Fatalf("Parse failed: %v", err)
	}

	if rm.Title != "spring-security-roadmap" {
		t.Errorf("Title = %q, want %q", rm.Title, "spring-security-roadmap")
	}

	if len(rm.Chapters) != 3 {
		t.Fatalf("Expected 3 chapters, got %d", len(rm.Chapters))
	}

	// ── Chapter 0: sub-chapters with topics (full 3-level depth) ──
	prereq := rm.Chapters[0]
	if prereq.Name != "prerequisites" {
		t.Errorf("Chapter 0 name = %q, want %q", prereq.Name, "prerequisites")
	}
	if len(prereq.SubChapters) != 2 {
		t.Fatalf("Chapter 0: expected 2 sub-chapters, got %d", len(prereq.SubChapters))
	}
	if prereq.SubChapters[0].Name != "java-web-basics" {
		t.Errorf("Sub-chapter name = %q, want %q", prereq.SubChapters[0].Name, "java-web-basics")
	}
	wantTopics := []string{"internet-vs-web", "client-server", "request-response"}
	got := prereq.SubChapters[0].Topics
	if len(got) != len(wantTopics) {
		t.Fatalf("Topics = %v, want %v", got, wantTopics)
	}
	for i := range wantTopics {
		if got[i] != wantTopics[i] {
			t.Errorf("Topic[%d] = %q, want %q", i, got[i], wantTopics[i])
		}
	}

	// ── Chapter 1: leaf items directly at sub-chapter level ──
	servlet := rm.Chapters[1]
	if servlet.Name != "servlet-world" {
		t.Errorf("Chapter 1 name = %q, want %q", servlet.Name, "servlet-world")
	}
	if len(servlet.SubChapters) != 3 {
		t.Fatalf("Chapter 1: expected 3 sub-chapters, got %d", len(servlet.SubChapters))
	}
	if servlet.SubChapters[0].Name != "servlet-api" {
		t.Errorf("Leaf sub-chapter name = %q, want %q", servlet.SubChapters[0].Name, "servlet-api")
	}
	if len(servlet.SubChapters[0].Topics) != 0 {
		t.Errorf("Leaf sub-chapter should have no topics, got %v", servlet.SubChapters[0].Topics)
	}

	// ── Chapter 2: mixed granularity in one chapter ──
	mixed := rm.Chapters[2]
	if mixed.Name != "mixed" {
		t.Errorf("Chapter 2 name = %q, want %q", mixed.Name, "mixed")
	}
	if len(mixed.SubChapters) != 2 {
		t.Fatalf("Chapter 2: expected 2 sub-chapters, got %d", len(mixed.SubChapters))
	}
	if mixed.SubChapters[0].Name != "grouped-topic" || len(mixed.SubChapters[0].Topics) != 2 {
		t.Errorf("Chapter 2 sub 0 = %q with topics %v", mixed.SubChapters[0].Name, mixed.SubChapters[0].Topics)
	}
	if mixed.SubChapters[1].Name != "leaf-topic" || len(mixed.SubChapters[1].Topics) != 0 {
		t.Errorf("Chapter 2 sub 1 = %q with topics %v", mixed.SubChapters[1].Name, mixed.SubChapters[1].Topics)
	}
}

// TestCleanName covers the name normalization used for numbered, file-based
// roadmaps (SpringSecurity.txt style).
func TestCleanName(t *testing.T) {
	cases := []struct{ in, want string }{
		{"01-java-web-basics/", "java-web-basics"},
		{"00-prerequisites/", "prerequisites"},
		{"05_underscore/", "underscore"},
		{"03.dotted", "dotted"},
		{"servlet-api.md", "servlet-api"},
		{"internet-vs-web.md", "internet-vs-web"},
		{"how-java-runs/", "how-java-runs"},
		{"source-code → bytecode → JVM", "source-code → bytecode → JVM"},
		{"README.markdown", "README"},
		{"notes.txt", "notes"},
		{"", ""},
		{"   ", ""},
	}
	for _, c := range cases {
		if got := cleanName(c.in); got != c.want {
			t.Errorf("cleanName(%q) = %q, want %q", c.in, got, c.want)
		}
	}
}


