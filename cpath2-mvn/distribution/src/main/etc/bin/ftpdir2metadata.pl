#!/usr/bin/perl

# author: rodche
# Generates cpath2 "metadata" configuration records from the ftp directory list 

use strict;
use warnings;
use NET::FTP;
use Time::localtime;

my ($host, $dir, $id, $typ, $co, $cl, $ver) = @ARGV;

$cl ||= "cpath.cleaner.internal.BaseCleanerImpl";
$co ||= "cpath.converter.internal.BaseConverterImpl";

die "Use Parameters: host, directory, (new metadata)identifier, type, [converter class, cleaner class, version]\n" 
	unless (@ARGV > 3);

my $tm = localtime;
$ver ||= join "", $tm->year+1900, $tm->mon+1;

my $today = join "", $tm->year+1900, $tm->mon+1, $tm->mday;

my $ftp = Net::FTP->new("$host", Debug => 0)
	or die "Cannot connect to some.host.name: $@";
      
$ftp->login("anonymous",'-anonymous@')
	or die "Cannot login ", $ftp->message;
$ftp->cwd("$dir") 
	or die "Cannot change working directory ", $ftp->message;

my @files = $ftp->ls() 
	or die "get failed ", $ftp->message;

#print join("\n",@files);

my $i = 0;
foreach (sort @files) {
	++$i;
	print join("<br>", "$id$i", "", $ver, $today, "ftp://$host$dir/$_", "", $typ, $cl, $co), "\n" 
		if ($_ =~ "\.gz" || $_ =~ "\.zip");
}

$ftp->quit;


