import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Celeris — preview interactive des popups",
  description:
    "Preview interactive des 26 genres de popups de l'éditeur Celeris — simulateur fidèle au PopupController (régimes, exclusions, sections des designs), catalogue documentaire par catégorie.",
  keywords: ["Celeris", "popups", "éditeur", "Android", "PopupController", "documentation interactive"],
  openGraph: {
    title: "Celeris — preview interactive des popups",
    description: "Les 26 genres de popups de l'éditeur, leurs sections et leurs régimes, en simulateur interactif",
    siteName: "Celeris",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-background text-foreground`}
      >
        {children}
      </body>
    </html>
  );
}
