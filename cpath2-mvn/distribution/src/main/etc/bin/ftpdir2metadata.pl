#!/usr/bin/perl

# author: rodche
#
# Generates cpath2 "metadata" configuration records from the ftp directory list 

use strict;
use warnings;
use NET::FTP;
use Time::localtime;

die "Use Parameters: host, directory, (new metadata)identifier, type, [version, cleaner class, converter class]\n" 
	unless (@ARGV > 3);

my ($host, $dir, $id, $type, $ver, $cleaner, $converter) = @ARGV;

$cleaner |= "cpath.cleaner.internal.BaseCleanerImpl";
$converter |= "cpath.converter.internal.BaseConverterImpl";

my $tm = localtime;
$ver |= join "", $tm->year+1900, $tm->mon+1;
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
foreach (@files) {
	++$i;
	print join("<br>", ("$id$i","",$ver, $today, "ftp://$host$dir/$_","",$type,$cleaner,$converter)), "\n"
		if $_ =~ "\.gz" || $_ =~ "\.zip";
}

$ftp->quit;


