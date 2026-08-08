import "./globals.css";

export const metadata = {
  title: "Note Factory — AI-Powered Study Notes Generator",
  description:
    "Transform any learning roadmap into comprehensive, textbook-quality study notes powered by AI. Perfect for students mastering programming, frameworks, and technical concepts.",
  keywords: ["study notes", "AI", "learning", "roadmap", "Java", "programming"],
  openGraph: {
    title: "Note Factory",
    description: "AI-Powered Study Notes Generator",
    type: "website",
  },
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <div id="app-root">{children}</div>
      </body>
    </html>
  );
}
